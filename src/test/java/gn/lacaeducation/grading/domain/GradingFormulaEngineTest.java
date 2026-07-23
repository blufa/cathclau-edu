package gn.lacaeducation.grading.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduit les exemples chiffrés de « Refonte complète du moteur de calcul des notes »
 * (features/specifications_logiciel.md) : moyenne mensuelle → moyenne de cours → moyenne
 * semestrielle, pour une matière de coefficient 1 puis de coefficient 2.
 */
class GradingFormulaEngineTest {

    private static final LocalDate PERIOD_START = LocalDate.of(2025, 10, 1);
    private static final GradingCalculationSettings SETTINGS = GradingCalculationSettings.defaults();

    /** Oct/Nov/Déc : orale=14, écrite=16 (moyenne mensuelle = 15 à chaque mois) ; composition = 13. */
    private List<Assessment> assessments(BigDecimal monthlyCoeff, BigDecimal compoCoeff) {
        return List.of(
                assessment(1L, LocalDate.of(2025, 10, 5), monthlyCoeff, null),
                assessment(2L, LocalDate.of(2025, 10, 20), monthlyCoeff, null),
                assessment(3L, LocalDate.of(2025, 11, 5), monthlyCoeff, null),
                assessment(4L, LocalDate.of(2025, 11, 20), monthlyCoeff, null),
                assessment(5L, LocalDate.of(2025, 12, 5), monthlyCoeff, null),
                assessment(6L, LocalDate.of(2025, 12, 20), monthlyCoeff, null),
                assessment(7L, LocalDate.of(2026, 1, 15), compoCoeff, 99L) // composition, type 99
        );
    }

    private Assessment assessment(Long id, LocalDate date, BigDecimal coeff, Long assessmentTypeId) {
        return Assessment.builder().id(id).date(date).coefficient(coeff).assessmentTypeId(assessmentTypeId).build();
    }

    private List<Grade> grades() {
        return List.of(
                grade(1L, "14"), grade(2L, "16"), // octobre
                grade(3L, "14"), grade(4L, "16"), // novembre
                grade(5L, "14"), grade(6L, "16"), // décembre
                grade(7L, "13"));                  // composition
    }

    private Grade grade(Long assessmentId, String value) {
        return Grade.builder().assessmentId(assessmentId).value(new BigDecimal(value)).build();
    }

    /** Étape 1 (mois) : GROUP par monthIndex 1/2/3. Étape 2 (composition) : GROUP par assessmentTypeId. */
    private GradingFormulaStep month(int order, int monthIndex) {
        return GradingFormulaStep.builder().stepOrder(order).stepKind(StepKind.GROUP).monthIndex(monthIndex).build();
    }

    private GradingFormulaStep compoStep(int order) {
        return GradingFormulaStep.builder().stepOrder(order).stepKind(StepKind.GROUP).assessmentTypeId(99L).build();
    }

    private GradingFormulaStep coursStep(int order, int... inputOrders) {
        List<GradingFormulaStepInput> inputs = java.util.Arrays.stream(inputOrders)
                .mapToObj(o -> GradingFormulaStepInput.builder().refStepOrder(o).weightMode(WeightMode.EQUAL).build())
                .toList();
        return GradingFormulaStep.builder().stepOrder(order).stepKind(StepKind.COMBINE).inputs(inputs).build();
    }

    @Test
    void coefficient1_semesterAverage_isFourteen() {
        // Moyenne de cours = (15+15+15)/3 = 15 ; semestrielle = (15 + 13)/2 = 14 (poids égaux, coefficient 1)
        List<GradingFormulaStep> steps = List.of(
                month(1, 1), month(2, 2), month(3, 3),
                coursStep(4, 1, 2, 3),
                compoStep(5),
                GradingFormulaStep.builder().stepOrder(6).stepKind(StepKind.COMBINE).finalStep(true)
                        .inputs(List.of(
                                GradingFormulaStepInput.builder().refStepOrder(4).weightMode(WeightMode.EQUAL).build(),
                                GradingFormulaStepInput.builder().refStepOrder(5).weightMode(WeightMode.SUBJECT_COEFFICIENT).build()))
                        .build());

        BigDecimal result = GradingFormulaEngine.compute(steps, assessments(BigDecimal.ONE, BigDecimal.ONE), grades(),
                BigDecimal.ONE, PERIOD_START, SETTINGS, java.util.Map.of());

        assertThat(result).isEqualByComparingTo("14.00");
    }

    @Test
    void coefficient2_semesterAverage_matchesSpecExample() {
        // Moyenne de cours = 15 (le coefficient 2 s'annule dans la moyenne pondérée d'un mois) ;
        // semestrielle = (15×1 + 13×2) / (1+2) = 41/3 = 13.67
        List<GradingFormulaStep> steps = List.of(
                month(1, 1), month(2, 2), month(3, 3),
                coursStep(4, 1, 2, 3),
                compoStep(5),
                GradingFormulaStep.builder().stepOrder(6).stepKind(StepKind.COMBINE).finalStep(true)
                        .inputs(List.of(
                                GradingFormulaStepInput.builder().refStepOrder(4).weightMode(WeightMode.EQUAL).build(),
                                GradingFormulaStepInput.builder().refStepOrder(5).weightMode(WeightMode.SUBJECT_COEFFICIENT).build()))
                        .build());

        BigDecimal result = GradingFormulaEngine.compute(steps, assessments(new BigDecimal("2"), new BigDecimal("2")), grades(),
                new BigDecimal("2"), PERIOD_START, SETTINGS, java.util.Map.of());

        assertThat(result).isEqualByComparingTo("13.67");
    }

    @Test
    void noSteps_reproducesLegacyFlatWeightedAverage() {
        // Sans formule configurée : moyenne pondérée classique sur toutes les évaluations de la période.
        // (14+16+14+16+14+16+13) / 7 = 103/7 = 14.71 (coefficients tous égaux à 1 -> équivaut à la simple)
        BigDecimal result = GradingFormulaEngine.compute(List.of(), assessments(BigDecimal.ONE, BigDecimal.ONE), grades(),
                BigDecimal.ONE, PERIOD_START, SETTINGS, java.util.Map.of());

        assertThat(result).isEqualByComparingTo("14.71");
    }

    @Test
    void singleGroupStepCoveringWholePeriod_isEquivalentToLegacyFallback() {
        GradingFormulaStep legacyEquivalent = GradingFormulaStep.builder()
                .stepOrder(1).stepKind(StepKind.GROUP).finalStep(true).build();

        BigDecimal viaFormula = GradingFormulaEngine.compute(List.of(legacyEquivalent),
                assessments(BigDecimal.ONE, BigDecimal.ONE), grades(), BigDecimal.ONE, PERIOD_START, SETTINGS, java.util.Map.of());
        BigDecimal viaFallback = GradingFormulaEngine.compute(List.of(),
                assessments(BigDecimal.ONE, BigDecimal.ONE), grades(), BigDecimal.ONE, PERIOD_START, SETTINGS, java.util.Map.of());

        assertThat(viaFormula).isEqualByComparingTo(viaFallback);
    }
}
