package gn.lacaeducation.grading.domain;

import gn.lacaeducation.grading.domain.SubjectAverageCalculator.WeightedGrade;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectAverageCalculatorTest {

    // Notes : 10 (coeff 1), 16 (coeff 3), 8 (coeff 1)
    private final List<WeightedGrade> grades = List.of(
            new WeightedGrade(new BigDecimal("10"), new BigDecimal("1"), null),
            new WeightedGrade(new BigDecimal("16"), new BigDecimal("3"), null),
            new WeightedGrade(new BigDecimal("8"), new BigDecimal("1"), null));

    @Test
    void weighted_isDefaultWeightedAverage() {
        // (10×1 + 16×3 + 8×1) / (1+3+1) = 66/5 = 13.20
        BigDecimal r = SubjectAverageCalculator.compute(AverageMethod.WEIGHTED, grades, 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("13.20");
    }

    @Test
    void simple_isArithmeticMean() {
        // (10 + 16 + 8) / 3 = 11.33
        BigDecimal r = SubjectAverageCalculator.compute(AverageMethod.SIMPLE, grades, 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("11.33");
    }

    @Test
    void bestScore_keepsHighest() {
        BigDecimal r = SubjectAverageCalculator.compute(AverageMethod.BEST_SCORE, grades, 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("16.00");
    }

    @Test
    void dropLowest_removesLowestThenWeighted() {
        // retire 8 (coeff 1) → (10×1 + 16×3)/(1+3) = 58/4 = 14.50
        BigDecimal r = SubjectAverageCalculator.compute(AverageMethod.DROP_LOWEST, grades, 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("14.50");
    }

    @Test
    void decimalsAndRounding_areApplied() {
        // simple = 11.333... ; 0 décimale, arrondi supérieur → 12
        BigDecimal r = SubjectAverageCalculator.compute(AverageMethod.SIMPLE, grades, 0, RoundingPolicy.UP);
        assertThat(r).isEqualByComparingTo("12");
        // arrondi inférieur → 11
        BigDecimal down = SubjectAverageCalculator.compute(AverageMethod.SIMPLE, grades, 0, RoundingPolicy.DOWN);
        assertThat(down).isEqualByComparingTo("11");
    }

    @Test
    void byCategory_weightsEachCategoryAverage() {
        // Catégorie 1 (poids 2) : moyenne pondérée de 10(coeff1) et 16(coeff3) = 58/4 = 14.5
        // Catégorie 2 (poids 1) : 8
        // Résultat : (14.5×2 + 8×1) / (2+1) = 37/3 = 12.33
        List<WeightedGrade> byCat = List.of(
                new WeightedGrade(new BigDecimal("10"), new BigDecimal("1"), 1L),
                new WeightedGrade(new BigDecimal("16"), new BigDecimal("3"), 1L),
                new WeightedGrade(new BigDecimal("8"), new BigDecimal("1"), 2L));
        java.util.Map<Long, BigDecimal> weights = java.util.Map.of(
                1L, new BigDecimal("2"), 2L, new BigDecimal("1"));
        BigDecimal r = SubjectAverageCalculator.compute(
                AverageMethod.BY_CATEGORY, byCat, weights, 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("12.33");
    }

    @Test
    void byCategory_withoutWeights_fallsBackToWeighted() {
        // Sans poids de catégorie configuré, on retombe sur la moyenne pondérée classique = 13.20
        BigDecimal r = SubjectAverageCalculator.compute(
                AverageMethod.BY_CATEGORY, grades, 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("13.20");
    }

    @Test
    void emptyGrades_returnsZero() {
        BigDecimal r = SubjectAverageCalculator.compute(AverageMethod.WEIGHTED, List.of(), 2, RoundingPolicy.MATHEMATICAL);
        assertThat(r).isEqualByComparingTo("0.00");
    }
}
