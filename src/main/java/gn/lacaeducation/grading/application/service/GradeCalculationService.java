package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.grading.application.event.ReportCardGeneratedEvent;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort.ClassSubjectInfo;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.GradingScaleRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.GradingScale;
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
    private final GradingScaleRepository gradingScaleRepository;
    private final gn.lacaeducation.grading.application.port.out.GradingCalculationSettingsRepository calculationSettingsRepository;
    private final gn.lacaeducation.grading.application.port.out.EliminatoryRuleRepository eliminatoryRuleRepository;
    private final gn.lacaeducation.grading.application.port.out.DecisionRuleRepository decisionRuleRepository;
    private final gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository assessmentTypeConfigRepository;
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
        gn.lacaeducation.grading.domain.GradingCalculationSettings calcSettings = resolveCalcSettings(assessments);
        Map<Long, BigDecimal> categoryWeights = resolveCategoryWeights(assessments);
        List<gn.lacaeducation.grading.domain.EliminatoryRule> eliminatoryRules = resolveEliminatoryRules(assessments);
        List<gn.lacaeducation.grading.domain.DecisionRule> decisionRules = resolveDecisionRules(assessments);

        // Build intermediate results (average without rank)
        List<ReportCard> reportCards = new ArrayList<>();
        for (Long studentId : studentIds) {
            ReportCardWithLines rcWithLines = buildReportCard(studentId, classId, periodId, assessments,
                    subjectInfos, scale, calcSettings, categoryWeights, eliminatoryRules, decisionRules);
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
                resolveScale(assessments), resolveCalcSettings(assessments), resolveCategoryWeights(assessments),
                resolveEliminatoryRules(assessments), resolveDecisionRules(assessments));
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
                                                GradingScale scale,
                                                gn.lacaeducation.grading.domain.GradingCalculationSettings calcSettings,
                                                Map<Long, BigDecimal> categoryWeights,
                                                List<gn.lacaeducation.grading.domain.EliminatoryRule> eliminatoryRules,
                                                List<gn.lacaeducation.grading.domain.DecisionRule> decisionRules) {

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

            BigDecimal subjectAverage = computeSubjectAverage(grades, subjectAssessments, calcSettings, categoryWeights);
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

        // Notes éliminatoires (§7) : une moyenne matière sous le seuil impose la décision de la règle.
        String decision = scale.decisionFor(generalAverage);
        java.util.Map<Long, BigDecimal> subjectAverages = lines.stream()
                .filter(l -> l.getSubjectId() != null && l.getSubjectAverage() != null)
                .collect(Collectors.toMap(ReportCardLine::getSubjectId, ReportCardLine::getSubjectAverage, (a, b) -> a));
        decision = gn.lacaeducation.grading.domain.EliminatoryRuleEvaluator.resolveDecision(
                decision, subjectAverages, eliminatoryRules);

        // Moteur de règles de décision paramétrable (§17) : une règle satisfaite impose sa décision.
        String ruleDecision = gn.lacaeducation.grading.domain.DecisionRuleEngine.evaluate(
                generalAverage, subjectAverages, decisionRules, java.time.LocalDate.now());
        if (ruleDecision != null) {
            decision = ruleDecision;
        }

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
     * Moyenne d'un élève dans une matière (sur les évaluations données), calculée avec exactement le
     * même paramétrage que les bulletins (méthode, décimales, arrondi, poids de catégorie). Renvoie
     * {@code null} si l'élève n'a aucune note. Exposée pour le relevé de notes par matière.
     */
    public BigDecimal subjectAverage(List<Assessment> subjectAssessments, List<Grade> studentGrades) {
        if (subjectAssessments == null || subjectAssessments.isEmpty()
                || studentGrades == null || studentGrades.isEmpty()) {
            return null;
        }
        var settings = resolveCalcSettings(subjectAssessments);
        var categoryWeights = resolveCategoryWeights(subjectAssessments);
        return computeSubjectAverage(studentGrades, subjectAssessments, settings, categoryWeights);
    }

    /**
     * Moyenne d'une matière selon la méthode et l'arrondi configurés par l'établissement (§2/§5).
     * Par défaut : moyenne pondérée par le coefficient d'évaluation, 2 décimales, arrondi mathématique.
     */
    private BigDecimal computeSubjectAverage(List<Grade> grades, List<Assessment> assessments,
                                             gn.lacaeducation.grading.domain.GradingCalculationSettings settings,
                                             Map<Long, BigDecimal> categoryWeights) {
        Map<Long, BigDecimal> coeffByAssessment = assessments.stream()
                .collect(Collectors.toMap(Assessment::getId,
                        a -> a.getCoefficient() != null ? a.getCoefficient() : BigDecimal.ONE));
        Map<Long, Long> categoryByAssessment = assessments.stream()
                .filter(a -> a.getAssessmentTypeId() != null)
                .collect(Collectors.toMap(Assessment::getId, Assessment::getAssessmentTypeId));

        List<gn.lacaeducation.grading.domain.SubjectAverageCalculator.WeightedGrade> weighted = grades.stream()
                .map(g -> new gn.lacaeducation.grading.domain.SubjectAverageCalculator.WeightedGrade(
                        g.getValue(), coeffByAssessment.getOrDefault(g.getAssessmentId(), BigDecimal.ONE),
                        categoryByAssessment.get(g.getAssessmentId())))
                .toList();

        return gn.lacaeducation.grading.domain.SubjectAverageCalculator.compute(
                settings.methodOrDefault(), weighted, categoryWeights,
                settings.decimalsOrDefault(), settings.roundingOrDefault());
    }

    /**
     * Poids de catégorie (type d'évaluation) du tenant, pour la méthode {@code BY_CATEGORY} :
     * map {@code assessmentTypeId -> poids}. Seuls les types actifs avec un poids strictement positif
     * sont retenus ; vide si non applicable (retombe alors sur la moyenne pondérée).
     */
    private Map<Long, BigDecimal> resolveCategoryWeights(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (tenantId == null) return Map.of();
        return assessmentTypeConfigRepository.findByTenantId(tenantId).stream()
                .filter(c -> c.isActive() && c.getWeight() != null && c.getWeight().signum() > 0)
                .collect(Collectors.toMap(
                        gn.lacaeducation.grading.domain.AssessmentTypeConfig::getId,
                        gn.lacaeducation.grading.domain.AssessmentTypeConfig::getWeight,
                        (a, b) -> a));
    }

    /** Réglage du moteur de calcul du tenant des évaluations (ou défauts). */
    private gn.lacaeducation.grading.domain.GradingCalculationSettings resolveCalcSettings(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (tenantId == null) return gn.lacaeducation.grading.domain.GradingCalculationSettings.defaults();
        return calculationSettingsRepository.findByTenantId(tenantId)
                .orElseGet(gn.lacaeducation.grading.domain.GradingCalculationSettings::defaults);
    }

    /** Règles de notes éliminatoires (§7) du tenant des évaluations. */
    private List<gn.lacaeducation.grading.domain.EliminatoryRule> resolveEliminatoryRules(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return tenantId == null ? List.of() : eliminatoryRuleRepository.findByTenantId(tenantId);
    }

    /** Règles de décision paramétrables (§17) du tenant des évaluations. */
    private List<gn.lacaeducation.grading.domain.DecisionRule> resolveDecisionRules(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return tenantId == null ? List.of() : decisionRuleRepository.findByTenantId(tenantId);
    }

    // -------------------------------------------------------------------------
    // Static helpers (package-private for tests)
    // -------------------------------------------------------------------------

    /** Resolves the tenant's grading scale from the assessments' tenant, falling back to defaults. */
    private GradingScale resolveScale(List<Assessment> assessments) {
        Long tenantId = assessments.stream()
                .map(Assessment::getTenantId).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (tenantId == null) return GradingScale.defaults();
        return gradingScaleRepository.findByTenantId(tenantId).orElseGet(GradingScale::defaults);
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
