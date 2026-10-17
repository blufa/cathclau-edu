package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.TeacherNameProvider;
import gn.lacaeducation.grading.application.event.ReportCardGeneratedEvent;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.CycleGroup;
import gn.lacaeducation.grading.domain.GradingScale;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import gn.lacaeducation.grading.domain.SubjectMonthlyBreakdown;
import gn.lacaeducation.grading.domain.SubjectSemesterAverage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core grade-calculation engine — standard et non paramétrable par établissement (§retour
 * pédagogique) : plus de barème/règles d'élimination/décision personnalisables, uniquement les
 * valeurs par défaut ({@link GradingScale#defaults(CycleGroup)}).
 *
 * Algorithm (CLAUDE.md §7.4) :
 * <pre>
 * subject_average   = voir {@link SubjectSemesterAverage} (formule distincte primaire/secondaire)
 * weighted_subject  = subject_average × subject_coefficient   (coefficient forcé à 1 en Primaire —
 *                     voir buildReportCard — donc weighted_subject = subject_average pour ce cycle)
 * general_average   = Σ(weighted_subject) / Σ(subject_coefficient)
 *                     — Secondaire : pondérée par les coefficients de matière (ClasseMatiere/subjects)
 *                     — Primaire   : Σ(subject_average) / nombre de matières notées (coefficients
 *                       neutralisés, voir ci-dessus)
 * rank              = position of student ordered by general_average desc within class+period (1 = best)
 * </pre>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GradeCalculationService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 2;

    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final ReportCardRepository reportCardRepository;
    private final ClassSubjectQueryPort classSubjectQueryPort;
    private final AcademicsDataProvider academicsDataProvider;
    private final TeacherNameProvider teacherNameProvider;
    private final ApplicationEventPublisher eventPublisher;

    /** Internal value object pairing a report card with its computed lines. */
    private record ReportCardWithLines(ReportCard reportCard, List<ReportCardLine> lines) {}

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generate (or regenerate) report cards for all enrolled students in a class + period.
     * Ranks are recalculated for all students after individual averages are computed.
     *
     * @return the saved report cards with ranks populated
     */
    public List<ReportCard> generateForClass(Long classId, Long periodId) {
        List<Assessment> assessments = assessmentRepository.findByClassIdAndPeriodId(classId, periodId);
        List<ClassSubjectInfo> subjectInfos = classSubjectQueryPort.findByClassId(classId);

        // Collect all distinct student IDs that have at least one grade
        List<Long> studentIds = assessments.stream()
                .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                .map(Grade::getStudentId)
                .distinct()
                .toList();

        if (studentIds.isEmpty()) {
            return List.of();
        }

        GradingScale scale = resolveScale(assessments);

        // Build intermediate results (average without rank)
        List<ReportCard> reportCards = new ArrayList<>();
        for (Long studentId : studentIds) {
            ReportCardWithLines rcWithLines = buildReportCard(studentId, classId, periodId, assessments,
                    subjectInfos, scale);
            ReportCard saved = persistReportCardWithLines(rcWithLines, periodId);
            reportCards.add(saved);
        }

        // Recalculate ranks and class statistics across all students
        updateRanksAndStats(reportCards);

        return reportCards;
    }

    /**
     * Generate (or regenerate) the report card for a single student in a class + period.
     * Triggers a full rank recalculation for the whole class so ranks stay consistent.
     *
     * @return the saved, fully populated report card
     */
    public ReportCard generateForStudent(Long studentId, Long classId, Long periodId) {
        List<Assessment> assessments = assessmentRepository.findByClassIdAndPeriodId(classId, periodId);
        List<ClassSubjectInfo> subjectInfos = classSubjectQueryPort.findByClassId(classId);

        ReportCardWithLines rcWithLines = buildReportCard(studentId, classId, periodId, assessments, subjectInfos,
                resolveScale(assessments));
        ReportCard saved = persistReportCardWithLines(rcWithLines, periodId);

        // Recalculate ranks for the whole class so the new card gets the right rank
        List<ReportCard> allInClass = reportCardRepository.findByClassIdAndPeriodId(classId, periodId);
        updateRanksAndStats(allInClass);

        // Reload to pick up updated rank
        return reportCardRepository.findById(saved.getId()).orElse(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds an in-memory (unsaved) ReportCard + its lines for one student.
     * Returns a {@link ReportCardWithLines} containing both; the domain object stays pure.
     */
    private ReportCardWithLines buildReportCard(Long studentId, Long classId, Long periodId,
                                                List<Assessment> assessments,
                                                List<ClassSubjectInfo> subjectInfos,
                                                GradingScale scale) {

        CycleGroup cycleGroup = resolveCycleGroup(classId);

        Map<Long, List<Assessment>> assessmentsBySubject = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getSubjectId));

        Map<Long, BigDecimal> subjectCoeffMap = subjectInfos.stream()
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::coefficient,
                        (a, b) -> a));

        Map<Long, Long> subjectTeacherMap = subjectInfos.stream()
                .filter(i -> i.teacherId() != null)
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::teacherId,
                        (a, b) -> a));

        List<ReportCardLine> lines = new ArrayList<>();
        BigDecimal sumWeighted = BigDecimal.ZERO;
        BigDecimal sumSubjectCoeffs = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Assessment>> entry : assessmentsBySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<Assessment> subjectAssessments = entry.getValue();

            List<Grade> grades = subjectAssessments.stream()
                    .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                    .filter(g -> studentId.equals(g.getStudentId()))
                    .toList();

            if (grades.isEmpty()) {
                continue;
            }

            SubjectSemesterAverage.Result formulaResult =
                    SubjectSemesterAverage.compute(cycleGroup, subjectAssessments, grades);
            BigDecimal subjectAverage = formulaResult.finalValue();
            if (subjectAverage == null) {
                continue; // notes présentes mais sans valeur exploitable (cas dégradé, voir domaine)
            }
            // Primaire (§retour pédagogique) : moyenne générale = simple moyenne des moyennes de
            // matière (Σ/nombre de matières), jamais pondérée par un coefficient — contrairement au
            // Secondaire. Forcer le coefficient à 1 ici fait que sumSubjectCoeffs ci-dessous vaut
            // exactement le nombre de matières notées, sans dupliquer la formule de division plus bas.
            BigDecimal subjectCoeff = cycleGroup == CycleGroup.PRIMAIRE
                    ? BigDecimal.ONE
                    : subjectCoeffMap.getOrDefault(subjectId, BigDecimal.ONE);
            BigDecimal weighted = subjectAverage.multiply(subjectCoeff, MC).setScale(SCALE, RoundingMode.HALF_UP);

            Long teacherId = subjectTeacherMap.get(subjectId);
            // Incident trouvé en testant en direct (§retour fondateur, école Notre Mère) : le bulletin
            // affichait littéralement "teacher-30" (placeholder jamais résolu) au lieu du nom réel.
            String teacherLabel = teacherId != null ? teacherNameProvider.teacherName(teacherId).orElse(null) : null;

            lines.add(ReportCardLine.builder()
                    .subjectId(subjectId)
                    .subjectAverage(subjectAverage)
                    .coefficient(subjectCoeff)
                    .weightedAverage(weighted)
                    .coursAverage(formulaResult.coursValue())
                    .compoAverage(formulaResult.compoValue())
                    .teacherName(teacherLabel)
                    .build());

            sumWeighted = sumWeighted.add(weighted);
            sumSubjectCoeffs = sumSubjectCoeffs.add(subjectCoeff);
        }

        BigDecimal generalAverage = computeOverallAverage(sumWeighted, sumSubjectCoeffs);

        // Décision standard, non paramétrable (§retour pédagogique) : issue uniquement du barème fixe
        // (voir GradingScale.defaults) — plus de règles d'élimination/décision personnalisables.
        String decision = scale.decisionFor(generalAverage);

        ReportCard rc = ReportCard.builder()
                .studentId(studentId)
                .classId(classId)
                .periodId(periodId)
                .overallAverage(generalAverage)
                .honor(scale.honorFor(generalAverage))
                .decision(decision)
                .distinction(scale.distinctionFor(generalAverage, minSubjectAverage(lines)))
                .status(ReportCardStatus.GENERATED)
                .generatedAt(LocalDateTime.now())
                .build();

        return new ReportCardWithLines(rc, lines);
    }

    /**
     * Persists a ReportCard (creating or replacing) together with its lines.
     * If a report card already exists for this student+period, it is replaced.
     */
    private ReportCard persistReportCardWithLines(ReportCardWithLines rcWithLines, Long periodId) {
        ReportCard rc = rcWithLines.reportCard();
        List<ReportCardLine> lines = rcWithLines.lines();

        Optional<ReportCard> existing = reportCardRepository.findByStudentIdAndPeriodId(rc.getStudentId(), periodId);
        existing.ifPresent(e -> {
            reportCardRepository.deleteLinesByReportCardId(e.getId());
            reportCardRepository.deleteById(e.getId());
        });

        ReportCard saved = reportCardRepository.save(rc);

        for (ReportCardLine line : lines) {
            line.setReportCardId(saved.getId());
            reportCardRepository.saveLine(line);
        }

        eventPublisher.publishEvent(new ReportCardGeneratedEvent(saved.getId(), saved.getStudentId(), null));

        return saved;
    }

    /**
     * Moyenne générale de l'élève (toutes matières confondues) — règle rigoureuse du système guinéen
     * (confirmée avec exemples chiffrés) : {@code Moyenne_Generale_Eleve = Σ(Points_Matiere) /
     * Σ(Coefficients_De_La_Classe)}, où {@code Points_Matiere = Moy_Matiere_Brute × coefficient de la
     * matière} (déjà accumulé dans {@code sumWeighted}/{@code sumSubjectCoeffs} par l'appelant). Le
     * coefficient n'intervient JAMAIS avant cette étape (ni dans Moy_Cours, ni dans Moy_Matiere_Brute).
     * <p>
     * Une version antérieure combinait séparément une « moyenne de cours générale » (simple entre
     * matières) et une « moyenne de composition générale » (pondérée par coefficient) — ça ne
     * correspondait pas à la règle confirmée (vérifié par calcul direct sur les exemples fournis : la
     * moyenne blended donnait 13.25 au lieu de 13.17 attendu) et a été retiré. Les colonnes
     * {@code coursAverage}/{@code compoAverage} par matière restent affichées sur le bulletin (détail
     * informatif), simplement plus utilisées pour la moyenne générale.
     */
    private BigDecimal computeOverallAverage(BigDecimal sumWeighted, BigDecimal sumSubjectCoeffs) {
        return sumSubjectCoeffs.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : sumWeighted.divide(sumSubjectCoeffs, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Recomputes ranks and class statistics for a set of report cards belonging to
     * the same class and period, then persists the updates.
     */
    private void updateRanksAndStats(List<ReportCard> reportCards) {
        if (reportCards.isEmpty()) return;

        List<ReportCard> sorted = reportCards.stream()
                .sorted(Comparator.comparing(ReportCard::getOverallAverage,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long validCount = sorted.stream()
                .map(ReportCard::getOverallAverage)
                .filter(a -> a != null)
                .count();

        BigDecimal classAvg = validCount == 0 ? BigDecimal.ZERO :
                sorted.stream()
                        .map(ReportCard::getOverallAverage)
                        .filter(a -> a != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(validCount), SCALE, RoundingMode.HALF_UP);

        BigDecimal highest = sorted.stream()
                .map(ReportCard::getOverallAverage)
                .filter(a -> a != null)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal lowest = sorted.stream()
                .map(ReportCard::getOverallAverage)
                .filter(a -> a != null)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        int classSize = sorted.size();

        // Ex æquo (§4, règle confirmée) : deux moyennes identiques partagent le même rang, et l'élève
        // suivant reprend au rang correspondant à sa position réelle (ex. 3, 3, 5 — jamais 3, 4, 5).
        // Un élève sans moyenne (aucune note) ne partage jamais son rang, y compris avec un autre élève
        // sans moyenne — cas limite non couvert par la règle, tranché pour rester simple et prévisible.
        int rank = 1;
        BigDecimal previousAverage = null;
        int position = 0;
        for (ReportCard rc : sorted) {
            position++;
            BigDecimal current = rc.getOverallAverage();
            if (previousAverage == null || current == null || previousAverage.compareTo(current) != 0) {
                rank = position;
            }
            rc.setRank(rank);
            previousAverage = current;
            rc.setClassAverage(classAvg);
            rc.setHighestAverage(highest);
            rc.setLowestAverage(lowest);
            rc.setClassSize(classSize);
            reportCardRepository.save(rc);
        }

        updateSubjectRanks(reportCards);
    }

    /**
     * Calculates per-subject ranks across all students for the given report cards — mêmes règles
     * d'ex æquo que le rang général (voir {@link #updateRanksAndStats}).
     */
    private void updateSubjectRanks(List<ReportCard> reportCards) {
        Map<Long, List<ReportCardLine>> linesBySubject = reportCards.stream()
                .flatMap(rc -> reportCardRepository.findLines(rc.getId()).stream())
                .collect(Collectors.groupingBy(ReportCardLine::getSubjectId));

        for (List<ReportCardLine> lines : linesBySubject.values()) {
            List<ReportCardLine> sortedLines = lines.stream()
                    .sorted(Comparator.comparing(ReportCardLine::getSubjectAverage,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            int subjectRank = 1;
            BigDecimal previousAverage = null;
            int position = 0;
            for (ReportCardLine line : sortedLines) {
                position++;
                BigDecimal current = line.getSubjectAverage();
                if (previousAverage == null || current == null || previousAverage.compareTo(current) != 0) {
                    subjectRank = position;
                }
                line.setSubjectRank(subjectRank);
                previousAverage = current;
                reportCardRepository.saveLine(line);
            }
        }
    }

    /**
     * Moyenne d'un élève dans une matière (sur les évaluations données), calculée avec exactement la
     * même formule que les bulletins (voir {@link SubjectSemesterAverage}). Renvoie {@code null} si
     * l'élève n'a aucune note exploitable. Exposée pour le relevé de notes par matière.
     */
    public BigDecimal subjectAverage(List<Assessment> subjectAssessments, List<Grade> studentGrades) {
        if (subjectAssessments == null || subjectAssessments.isEmpty()
                || studentGrades == null || studentGrades.isEmpty()) {
            return null;
        }
        Long classId = subjectAssessments.stream()
                .map(Assessment::getClassId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return SubjectSemesterAverage.compute(resolveCycleGroup(classId), subjectAssessments, studentGrades)
                .finalValue();
    }

    /** Détail mensuel "cours" par matière d'un élève (bulletin §colonnes par mois) — recalculé à la
     * volée (même pattern que {@code classCentralizerPdf}/{@code subjectGradeReport}), aucune
     * persistance. Toujours vide en Primaire (pas de notion de "cours" dans ce cycle). */
    @Transactional(readOnly = true)
    public List<SubjectMonthlyBreakdown> monthlyBreakdown(Long studentId, Long classId, Long periodId) {
        CycleGroup cycleGroup = resolveCycleGroup(classId);
        if (cycleGroup == CycleGroup.PRIMAIRE) {
            return List.of();
        }
        List<Assessment> assessments = assessmentRepository.findByClassIdAndPeriodId(classId, periodId);
        Map<Long, List<Assessment>> assessmentsBySubject = assessments.stream()
                .collect(Collectors.groupingBy(Assessment::getSubjectId));
        Map<Long, BigDecimal> subjectCoeffMap = classSubjectQueryPort.findByClassId(classId).stream()
                .collect(Collectors.toMap(ClassSubjectInfo::subjectId, ClassSubjectInfo::coefficient, (a, b) -> a));

        List<SubjectMonthlyBreakdown> result = new ArrayList<>();
        for (Map.Entry<Long, List<Assessment>> entry : assessmentsBySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<Grade> grades = entry.getValue().stream()
                    .flatMap(a -> gradeRepository.findByAssessmentId(a.getId()).stream())
                    .filter(g -> studentId.equals(g.getStudentId()))
                    .toList();
            if (grades.isEmpty()) continue;
            SubjectSemesterAverage.Result formulaResult = SubjectSemesterAverage.compute(cycleGroup, entry.getValue(), grades);
            result.add(new SubjectMonthlyBreakdown(subjectId, subjectCoeffMap.getOrDefault(subjectId, BigDecimal.ONE),
                    formulaResult.monthlyCours(), formulaResult.compoValue(), formulaResult.finalValue()));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Static helpers (package-private for tests)
    // -------------------------------------------------------------------------

    /**
     * Barème standard, non paramétrable (§retour pédagogique) : plus de surcharge par tenant — la
     * seule variable est le groupe de cycle (primaire/secondaire), qui détermine les bornes par
     * défaut à utiliser (voir {@link GradingScale#defaults(CycleGroup)}).
     */
    private GradingScale resolveScale(List<Assessment> assessments) {
        Long classId = assessments.stream()
                .map(Assessment::getClassId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return GradingScale.defaults(resolveCycleGroup(classId));
    }

    /** Groupe de cycle (primaire/secondaire) de la classe, résolu via son niveau (module academics). */
    private CycleGroup resolveCycleGroup(Long classId) {
        if (classId == null) return CycleGroup.SECONDAIRE;
        return academicsDataProvider.findClass(classId)
                .map(c -> CycleGroup.fromSchoolCycle(c.schoolCycle()))
                .orElse(CycleGroup.SECONDAIRE);
    }

    /** Plus faible moyenne de matière des lignes du bulletin, ou {@code null} si aucune. */
    private static BigDecimal minSubjectAverage(List<ReportCardLine> lines) {
        return lines.stream()
                .map(ReportCardLine::getSubjectAverage)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
}
