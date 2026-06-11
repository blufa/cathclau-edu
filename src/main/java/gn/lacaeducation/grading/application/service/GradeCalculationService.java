package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.grading.application.event.ReportCardGeneratedEvent;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
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
 * Core grade-calculation engine.
 *
 * Algorithm (CLAUDE.md §7.4):
 * <pre>
 * subject_average   = Σ(grade.value × assessment.coefficient) / Σ(assessment.coefficient)
 * weighted_subject  = subject_average × subject_coefficient   (from ClasseMatiere / subjects table)
 * general_average   = Σ(weighted_subject) / Σ(subject_coefficient)
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

        // Build intermediate results (average without rank)
        List<ReportCard> reportCards = new ArrayList<>();
        for (Long studentId : studentIds) {
            ReportCardWithLines rcWithLines = buildReportCard(studentId, classId, periodId, assessments, subjectInfos);
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

        ReportCardWithLines rcWithLines = buildReportCard(studentId, classId, periodId, assessments, subjectInfos);
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
                                                List<ClassSubjectInfo> subjectInfos) {

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

            BigDecimal subjectAverage = computeSubjectAverage(grades, subjectAssessments);
            BigDecimal subjectCoeff = subjectCoeffMap.getOrDefault(subjectId, BigDecimal.ONE);
            BigDecimal weighted = subjectAverage.multiply(subjectCoeff, MC).setScale(SCALE, RoundingMode.HALF_UP);

            Long teacherId = subjectTeacherMap.get(subjectId);
            String teacherLabel = teacherId != null ? "teacher-" + teacherId : null;

            lines.add(ReportCardLine.builder()
                    .subjectId(subjectId)
                    .subjectAverage(subjectAverage)
                    .coefficient(subjectCoeff)
                    .weightedAverage(weighted)
                    .teacherName(teacherLabel)
                    .build());

            sumWeighted = sumWeighted.add(weighted);
            sumSubjectCoeffs = sumSubjectCoeffs.add(subjectCoeff);
        }

        BigDecimal generalAverage = sumSubjectCoeffs.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : sumWeighted.divide(sumSubjectCoeffs, SCALE, RoundingMode.HALF_UP);

        ReportCard rc = ReportCard.builder()
                .studentId(studentId)
                .classId(classId)
                .periodId(periodId)
                .overallAverage(generalAverage)
                .honor(computeHonor(generalAverage))
                .decision(computeDecision(generalAverage))
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

        int rank = 1;
        for (ReportCard rc : sorted) {
            rc.setRank(rank++);
            rc.setClassAverage(classAvg);
            rc.setHighestAverage(highest);
            rc.setLowestAverage(lowest);
            rc.setClassSize(classSize);
            reportCardRepository.save(rc);
        }

        updateSubjectRanks(reportCards);
    }

    /**
     * Calculates per-subject ranks across all students for the given report cards.
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
            for (ReportCardLine line : sortedLines) {
                line.setSubjectRank(subjectRank++);
                reportCardRepository.saveLine(line);
            }
        }
    }

    /**
     * Weighted average of grades for one subject:
     * subject_average = Σ(grade.value × assessment.coefficient) / Σ(assessment.coefficient)
     */
    private BigDecimal computeSubjectAverage(List<Grade> grades, List<Assessment> assessments) {
        Map<Long, BigDecimal> coeffByAssessment = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId,
                        a -> a.getCoefficient() != null ? a.getCoefficient() : BigDecimal.ONE));

        BigDecimal sumNumerator = BigDecimal.ZERO;
        BigDecimal sumDenominator = BigDecimal.ZERO;

        for (Grade g : grades) {
            BigDecimal coeff = coeffByAssessment.getOrDefault(g.getAssessmentId(), BigDecimal.ONE);
            sumNumerator = sumNumerator.add(g.getValue().multiply(coeff, MC));
            sumDenominator = sumDenominator.add(coeff);
        }

        if (sumDenominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sumNumerator.divide(sumDenominator, SCALE, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Static helpers (package-private for tests)
    // -------------------------------------------------------------------------

    /**
     * Mention/honor based on general average out of 20.
     */
    static String computeHonor(BigDecimal average) {
        if (average == null) return "Insuffisant";
        if (average.compareTo(BigDecimal.valueOf(18)) >= 0) return "Très Bien";
        if (average.compareTo(BigDecimal.valueOf(16)) >= 0) return "Bien";
        if (average.compareTo(BigDecimal.valueOf(14)) >= 0) return "Assez Bien";
        if (average.compareTo(BigDecimal.valueOf(10)) >= 0) return "Passable";
        return "Insuffisant";
    }

    /**
     * Admission decision based on the 10/20 pass threshold.
     */
    static String computeDecision(BigDecimal average) {
        if (average == null) return "REDOUBLE";
        return average.compareTo(BigDecimal.valueOf(10)) >= 0 ? "ADMIS" : "REDOUBLE";
    }
}
