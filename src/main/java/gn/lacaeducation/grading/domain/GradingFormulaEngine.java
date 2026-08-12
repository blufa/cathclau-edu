package gn.lacaeducation.grading.domain;

import gn.lacaeducation.grading.domain.SubjectAverageCalculator.WeightedGrade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Exécute une {@link GradingFormula} pour calculer la moyenne d'une matière (§ Refonte du moteur de
 * calcul des notes). Fonction pure : ni JPA ni Spring. Chaque étape {@code GROUP} ou {@code COMBINE}
 * délègue le calcul de moyenne à {@link SubjectAverageCalculator}, seul endroit où vivent les
 * méthodes de moyenne (simple, pondérée, meilleure note...) — cette classe se contente d'assembler
 * les groupes d'évaluations et les entrées à combiner selon la formule configurée.
 * <p>
 * Sans étape (formule absente ou vide), reproduit exactement l'ancien calcul plat : moyenne pondérée
 * de toutes les évaluations de la période, filet de sécurité utilisé tant qu'un tenant n'a pas encore
 * de formule migrée.
 */
public final class GradingFormulaEngine {

    private GradingFormulaEngine() {}

    /**
     * Résultat détaillé d'une formule : la valeur finale (moyenne de matière), et, si des étapes sont
     * étiquetées {@link StepRole#COURS}/{@link StepRole#COMPOSITION}, leurs résultats respectifs
     * (sinon {@code null}) — utilisés par {@code GradeCalculationService} pour la moyenne générale.
     */
    public record SubjectFormulaResult(BigDecimal finalValue, BigDecimal coursValue, BigDecimal compoValue) {}

    public static BigDecimal compute(List<GradingFormulaStep> steps,
                                     List<Assessment> assessments,
                                     List<Grade> studentGrades,
                                     BigDecimal subjectCoefficient,
                                     LocalDate periodStart,
                                     GradingCalculationSettings settings,
                                     Map<Long, BigDecimal> categoryWeights) {
        return computeDetailed(steps, assessments, studentGrades, subjectCoefficient, periodStart, settings, categoryWeights)
                .finalValue();
    }

    public static SubjectFormulaResult computeDetailed(List<GradingFormulaStep> steps,
                                     List<Assessment> assessments,
                                     List<Grade> studentGrades,
                                     BigDecimal subjectCoefficient,
                                     LocalDate periodStart,
                                     GradingCalculationSettings settings,
                                     Map<Long, BigDecimal> categoryWeights) {
        List<Assessment> periodAssessments = assessments != null ? assessments : List.of();
        Map<Long, Grade> gradeByAssessment = (studentGrades != null ? studentGrades.stream() : List.<Grade>of().stream())
                .collect(java.util.stream.Collectors.toMap(Grade::getAssessmentId, g -> g, (a, b) -> a));

        if (steps == null || steps.isEmpty()) {
            BigDecimal result = computeGroup(GradingFormulaStep.builder().build(), periodAssessments, gradeByAssessment,
                    periodStart, settings, categoryWeights);
            return new SubjectFormulaResult(result, null, null);
        }

        List<GradingFormulaStep> ordered = steps.stream()
                .sorted(Comparator.comparingInt(GradingFormulaStep::getStepOrder))
                .toList();

        Map<Integer, BigDecimal> resultsByOrder = new LinkedHashMap<>();
        BigDecimal finalResult = BigDecimal.ZERO;
        BigDecimal coursValue = null;
        BigDecimal compoValue = null;
        for (GradingFormulaStep step : ordered) {
            BigDecimal result = step.getStepKind() == StepKind.COMBINE
                    ? computeCombine(step, resultsByOrder, subjectCoefficient, settings)
                    : computeGroup(step, periodAssessments, gradeByAssessment, periodStart, settings, categoryWeights);
            resultsByOrder.put(step.getStepOrder(), result);
            if (step.isFinalStep()) {
                finalResult = result;
            }
            StepRole role = step.getRole();
            if (role == StepRole.COURS) coursValue = result;
            else if (role == StepRole.COMPOSITION) compoValue = result;
        }
        return new SubjectFormulaResult(finalResult, coursValue, compoValue);
    }

    /** Moyenne des évaluations correspondant au filtre de l'étape (mois, type d'évaluation, ou tout si aucun filtre). */
    private static BigDecimal computeGroup(GradingFormulaStep step, List<Assessment> assessments,
                                           Map<Long, Grade> gradeByAssessment, LocalDate periodStart,
                                           GradingCalculationSettings settings, Map<Long, BigDecimal> categoryWeights) {
        List<WeightedGrade> weighted = assessments.stream()
                .filter(a -> matches(step, a, periodStart))
                .map(a -> {
                    Grade g = gradeByAssessment.get(a.getId());
                    if (g == null || g.getValue() == null) return null;
                    BigDecimal coeff = a.getCoefficient() != null ? a.getCoefficient() : BigDecimal.ONE;
                    return new WeightedGrade(g.getValue(), coeff, a.getAssessmentTypeId());
                })
                .filter(Objects::nonNull)
                .toList();
        return SubjectAverageCalculator.compute(settings.methodOrDefault(), weighted, categoryWeights,
                settings.decimalsOrDefault(), settings.roundingOrDefault());
    }

    private static boolean matches(GradingFormulaStep step, Assessment a, LocalDate periodStart) {
        if (step.getAssessmentTypeId() != null) {
            return step.getAssessmentTypeId().equals(a.getAssessmentTypeId());
        }
        if (step.getMonthIndex() != null) {
            return step.getMonthIndex().equals(monthIndex(a.getDate(), periodStart));
        }
        return true;
    }

    /** N-ième mois (1-based) de la date par rapport au début de la période ; 1 si la période n'a pas de date de début. */
    private static Integer monthIndex(LocalDate date, LocalDate periodStart) {
        if (date == null) return null;
        if (periodStart == null) return 1;
        int months = (date.getYear() - periodStart.getYear()) * 12 + (date.getMonthValue() - periodStart.getMonthValue());
        return months + 1;
    }

    /** Moyenne pondérée des résultats d'étapes précédentes, chaque entrée ayant son propre poids. */
    private static BigDecimal computeCombine(GradingFormulaStep step, Map<Integer, BigDecimal> resultsByOrder,
                                             BigDecimal subjectCoefficient, GradingCalculationSettings settings) {
        List<GradingFormulaStepInput> inputs = step.getInputs() != null ? step.getInputs() : List.of();
        List<WeightedGrade> pseudoGrades = inputs.stream()
                .map(input -> {
                    BigDecimal value = resultsByOrder.get(input.getRefStepOrder());
                    if (value == null) return null;
                    return new WeightedGrade(value, resolveWeight(input, subjectCoefficient), null);
                })
                .filter(Objects::nonNull)
                .toList();
        return SubjectAverageCalculator.compute(AverageMethod.WEIGHTED, pseudoGrades,
                settings.decimalsOrDefault(), settings.roundingOrDefault());
    }

    private static BigDecimal resolveWeight(GradingFormulaStepInput input, BigDecimal subjectCoefficient) {
        WeightMode mode = input.getWeightMode() != null ? input.getWeightMode() : WeightMode.EQUAL;
        return switch (mode) {
            case EQUAL -> BigDecimal.ONE;
            case SUBJECT_COEFFICIENT -> subjectCoefficient != null ? subjectCoefficient : BigDecimal.ONE;
            case FIXED -> input.getFixedWeight() != null ? input.getFixedWeight() : BigDecimal.ONE;
        };
    }
}
