package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.PeriodPublicInfo;
import gn.lacaeducation.grading.AttendanceSummaryProvider;
import gn.lacaeducation.grading.application.port.in.AnnualReportCardUseCase;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.AnnualReportCard;
import gn.lacaeducation.grading.domain.AnnualReportCardLine;
import gn.lacaeducation.grading.domain.GradingScale;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bulletin annuel (§documents ministère) : agrège à la volée les bulletins des deux semestres d'une
 * année scolaire — jamais persisté (pure agrégation de l'existant, cf. plan). Par matière et au
 * global, la moyenne annuelle est la moyenne simple des deux moyennes semestrielles (§5) ; si un seul
 * semestre a un bulletin généré, sa moyenne est utilisée telle quelle (année en cours).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnnualReportCardService implements AnnualReportCardUseCase {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

    private final ReportCardRepository reportCardRepository;
    private final AcademicsDataProvider academicsDataProvider;
    private final StudentDataProvider studentDataProvider;
    private final AttendanceSummaryProvider attendanceSummaryProvider;
    private final GradeCalculationService calculationService;
    private final gn.lacaeducation.grading.application.port.out.AnnualReportCardPdfGenerator pdfGenerator;

    @Override
    public List<AnnualReportCard> findByClass(Long tenantId, Long classId, Long academicYearId) {
        List<PeriodPublicInfo> semesters = resolveSemesterPeriods(academicYearId);
        if (semesters.isEmpty()) {
            return List.of();
        }
        PeriodPublicInfo s1 = semesters.get(0);
        PeriodPublicInfo s2 = semesters.size() > 1 ? semesters.get(1) : null;
        Long s1Id = s1.id();
        Long s2Id = s2 != null ? s2.id() : null;
        // Bloc BILAN (§retour fondateur) : absences/retards sur toute l'année en cours — du début du
        // 1er semestre à la fin du 2ème s'il existe déjà, sinon jusqu'à aujourd'hui (année en cours).
        java.time.LocalDate attendanceTo = s2 != null ? s2.endDate() : java.time.LocalDate.now();

        Map<Long, ReportCard> s1ByStudent = reportCardRepository.findByClassIdAndPeriodId(classId, s1Id).stream()
                .collect(Collectors.toMap(ReportCard::getStudentId, rc -> rc, (a, b) -> a));
        Map<Long, ReportCard> s2ByStudent = s2Id == null ? Map.of()
                : reportCardRepository.findByClassIdAndPeriodId(classId, s2Id).stream()
                        .collect(Collectors.toMap(ReportCard::getStudentId, rc -> rc, (a, b) -> a));

        Set<Long> studentIds = new LinkedHashSet<>();
        studentIds.addAll(s1ByStudent.keySet());
        studentIds.addAll(s2ByStudent.keySet());
        if (studentIds.isEmpty()) {
            return List.of();
        }

        gn.lacaeducation.grading.domain.CycleGroup cycleGroup = academicsDataProvider.findClass(classId)
                .map(c -> gn.lacaeducation.grading.domain.CycleGroup.fromSchoolCycle(c.schoolCycle()))
                .orElse(gn.lacaeducation.grading.domain.CycleGroup.SECONDAIRE);
        GradingScale scale = GradingScale.defaults(cycleGroup);

        List<AnnualReportCard> results = new ArrayList<>();
        for (Long studentId : studentIds) {
            results.add(build(studentId, classId, academicYearId, s1Id, s2Id,
                    s1ByStudent.get(studentId), s2ByStudent.get(studentId), scale,
                    s1.startDate(), attendanceTo));
        }
        rankAndStats(results);
        return results;
    }

    @Override
    public Optional<AnnualReportCard> findByStudent(Long tenantId, Long studentId, Long classId, Long academicYearId) {
        return findByClass(tenantId, classId, academicYearId).stream()
                .filter(a -> studentId.equals(a.getStudentId()))
                .findFirst();
    }

    @Override
    public byte[] pdfBytes(Long tenantId, Long studentId, Long classId, Long academicYearId) {
        AnnualReportCard card = findByStudent(tenantId, studentId, classId, academicYearId)
                .orElseThrow(() -> new gn.lacaeducation.common.ResourceNotFoundException("AnnualReportCard", studentId));
        return pdfGenerator.generate(card, buildContext(tenantId, card));
    }

    /** Résout les noms depuis les autres modules (élève, classe, matières, semestres) pour le PDF. */
    private gn.lacaeducation.grading.application.port.out.AnnualReportCardPdfGenerator.AnnualReportCardContext
            buildContext(Long tenantId, AnnualReportCard card) {
        var student = studentDataProvider.findStudent(card.getStudentId()).orElse(null);
        String studentName = student == null ? null
                : ((student.firstName() != null ? student.firstName() : "") + " "
                   + (student.lastName() != null ? student.lastName() : "")).trim();
        String className = academicsDataProvider.findClass(card.getClassId())
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String s1Label = card.getS1PeriodId() == null ? null
                : academicsDataProvider.findPeriod(card.getS1PeriodId()).map(PeriodPublicInfo::label).orElse(null);
        String s2Label = card.getS2PeriodId() == null ? null
                : academicsDataProvider.findPeriod(card.getS2PeriodId()).map(PeriodPublicInfo::label).orElse(null);
        String schoolYear = tenantId == null ? null : academicsDataProvider.currentAcademicYearLabel(tenantId).orElse(null);
        Set<Long> subjectIds = card.getLines() == null ? Set.of()
                : card.getLines().stream().map(AnnualReportCardLine::getSubjectId)
                        .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        var s2MonthlyBreakdown = card.getS2PeriodId() == null ? List.<gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown>of()
                : calculationService.monthlyBreakdown(card.getStudentId(), card.getClassId(), card.getS2PeriodId());
        return new gn.lacaeducation.grading.application.port.out.AnnualReportCardPdfGenerator.AnnualReportCardContext(
                tenantId, studentName, student != null ? student.studentNumber() : null,
                className, s1Label, s2Label, schoolYear, academicsDataProvider.subjectLabels(subjectIds),
                s2MonthlyBreakdown);
    }

    /** Les deux périodes de type SEMESTRE de l'année scolaire, triées par ordre (S1 puis S2). */
    /**
     * Les deux "vraies" périodes semestrielles de l'année (S1 puis S2), pour l'agrégation du bulletin
     * annuel. Certaines écoles déclarent 4 périodes de type SEMESTRE (ex. "1er Semestre" + "Compo 1er
     * Semestre" + "2ème Semestre" + "Compo Fin Année", école Notre Mère, §retour fondateur) — seules
     * celles qui SONT la source d'un lien {@code compositionPeriodId} comptent comme un "vrai" semestre
     * pour cette agrégation (leur composition associée est fusionnée en amont, voir
     * {@code GradeCalculationService#assessmentsForBulletin}, pas traitée comme un 3ème/4ème semestre
     * séparé). Repli sur le comportement historique (toutes les périodes SEMESTRE, dans l'ordre) si
     * aucune n'utilise cette convention — écoles à 2 périodes SEMESTRE simples, inchangées.
     */
    private List<PeriodPublicInfo> resolveSemesterPeriods(Long academicYearId) {
        List<PeriodPublicInfo> allSemesters = academicsDataProvider.findPeriodsByAcademicYear(academicYearId).stream()
                .filter(p -> "SEMESTRE".equals(p.type()))
                .toList();
        List<PeriodPublicInfo> withComposition = allSemesters.stream()
                .filter(p -> p.compositionPeriodId() != null)
                .toList();
        return withComposition.isEmpty() ? allSemesters : withComposition;
    }

    private AnnualReportCard build(Long studentId, Long classId, Long academicYearId, Long s1Id, Long s2Id,
                                   ReportCard s1, ReportCard s2, GradingScale scale,
                                   java.time.LocalDate attendanceFrom, java.time.LocalDate attendanceTo) {
        Map<Long, ReportCardLine> s1Lines = s1 == null ? Map.of()
                : reportCardRepository.findLines(s1.getId()).stream()
                        .collect(Collectors.toMap(ReportCardLine::getSubjectId, l -> l, (a, b) -> a));
        Map<Long, ReportCardLine> s2Lines = s2 == null ? Map.of()
                : reportCardRepository.findLines(s2.getId()).stream()
                        .collect(Collectors.toMap(ReportCardLine::getSubjectId, l -> l, (a, b) -> a));

        Set<Long> subjectIds = new LinkedHashSet<>();
        subjectIds.addAll(s1Lines.keySet());
        subjectIds.addAll(s2Lines.keySet());

        List<AnnualReportCardLine> lines = new ArrayList<>();
        for (Long subjectId : subjectIds) {
            ReportCardLine l1 = s1Lines.get(subjectId);
            ReportCardLine l2 = s2Lines.get(subjectId);
            BigDecimal annualAverage = average(l1 != null ? l1.getSubjectAverage() : null,
                    l2 != null ? l2.getSubjectAverage() : null);
            BigDecimal coefficient = l1 != null ? l1.getCoefficient() : (l2 != null ? l2.getCoefficient() : BigDecimal.ONE);
            lines.add(AnnualReportCardLine.builder()
                    .subjectId(subjectId)
                    .coefficient(coefficient)
                    .s1Cours(l1 != null ? l1.getCoursAverage() : null)
                    .s1Compo(l1 != null ? l1.getCompoAverage() : null)
                    .s1Average(l1 != null ? l1.getSubjectAverage() : null)
                    .s2Cours(l2 != null ? l2.getCoursAverage() : null)
                    .s2Compo(l2 != null ? l2.getCompoAverage() : null)
                    .s2Average(l2 != null ? l2.getSubjectAverage() : null)
                    .annualAverage(annualAverage)
                    .build());
        }

        BigDecimal overallAverage = average(s1 != null ? s1.getOverallAverage() : null,
                s2 != null ? s2.getOverallAverage() : null);

        String decision = scale.decisionFor(overallAverage);

        var attendance = attendanceSummaryProvider.summary(studentId, attendanceFrom, attendanceTo);

        return AnnualReportCard.builder()
                .studentId(studentId).classId(classId).academicYearId(academicYearId)
                .s1PeriodId(s1Id).s2PeriodId(s2Id)
                .lines(lines)
                .s1Average(s1 != null ? s1.getOverallAverage() : null)
                .s2Average(s2 != null ? s2.getOverallAverage() : null)
                .overallAverage(overallAverage)
                .honor(scale.honorFor(overallAverage))
                .decision(decision)
                .absentCount(attendance.absentCount())
                .lateCount(attendance.lateCount())
                .build();
    }

    /** Moyenne simple des deux valeurs non nulles ; si une seule est renseignée, elle est utilisée telle quelle. */
    private static BigDecimal average(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return null;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b, MC).divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
    }

    private void rankAndStats(List<AnnualReportCard> cards) {
        List<AnnualReportCard> sorted = cards.stream()
                .sorted(Comparator.comparing(AnnualReportCard::getOverallAverage,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int classSize = sorted.size();
        int rank = 1;
        for (AnnualReportCard c : sorted) {
            c.setRank(rank++);
            c.setClassSize(classSize);
        }
    }
}
