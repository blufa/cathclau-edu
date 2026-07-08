package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Moteur de calcul de la moyenne d'une matière (§5). Applique la méthode configurée par
 * l'établissement puis arrondit selon les paramètres (§2). Fonction pure, sans dépendance framework.
 */
public final class SubjectAverageCalculator {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    /** Une note avec son coefficient d'évaluation et sa catégorie (type d'évaluation, pour BY_CATEGORY). */
    public record WeightedGrade(BigDecimal value, BigDecimal coefficient, Long categoryId) {}

    private SubjectAverageCalculator() {}

    /** Variante sans poids de catégorie (méthodes autres que BY_CATEGORY). */
    public static BigDecimal compute(AverageMethod method, List<WeightedGrade> grades,
                                     int decimals, RoundingPolicy rounding) {
        return compute(method, grades, Map.of(), decimals, rounding);
    }

    public static BigDecimal compute(AverageMethod method, List<WeightedGrade> grades,
                                     Map<Long, BigDecimal> categoryWeights, int decimals, RoundingPolicy rounding) {
        if (grades == null || grades.isEmpty()) {
            return round(BigDecimal.ZERO, decimals, rounding);
        }
        AverageMethod m = method != null ? method : AverageMethod.WEIGHTED;
        BigDecimal raw = switch (m) {
            case SIMPLE -> simple(grades);
            case WEIGHTED -> weighted(grades);
            case BEST_SCORE -> best(grades);
            case DROP_LOWEST -> dropLowest(grades);
            case BY_CATEGORY -> byCategory(grades, categoryWeights);
        };
        return round(raw, decimals, rounding);
    }

    /**
     * Moyenne par catégorie : moyenne pondérée de chaque catégorie, puis pondération par le poids de
     * chaque catégorie. Les catégories sans poids configuré sont ignorées ; si aucun poids n'est
     * disponible, on retombe sur la moyenne pondérée classique.
     */
    private static BigDecimal byCategory(List<WeightedGrade> grades, Map<Long, BigDecimal> categoryWeights) {
        Map<Long, BigDecimal> weights = categoryWeights != null ? categoryWeights : Map.of();
        Map<Long, List<WeightedGrade>> byCat = new java.util.LinkedHashMap<>();
        for (WeightedGrade g : grades) {
            byCat.computeIfAbsent(g.categoryId(), k -> new java.util.ArrayList<>()).add(g);
        }
        BigDecimal num = BigDecimal.ZERO;
        BigDecimal den = BigDecimal.ZERO;
        for (Map.Entry<Long, List<WeightedGrade>> e : byCat.entrySet()) {
            BigDecimal weight = e.getKey() == null ? null : weights.get(e.getKey());
            if (weight == null || weight.signum() <= 0) continue;
            BigDecimal categoryAvg = weighted(e.getValue());
            num = num.add(categoryAvg.multiply(weight, MC));
            den = den.add(weight);
        }
        return den.signum() == 0 ? weighted(grades) : num.divide(den, MC);
    }

    private static BigDecimal simple(List<WeightedGrade> grades) {
        BigDecimal sum = grades.stream().map(WeightedGrade::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(grades.size()), MC);
    }

    private static BigDecimal weighted(List<WeightedGrade> grades) {
        BigDecimal num = BigDecimal.ZERO;
        BigDecimal den = BigDecimal.ZERO;
        for (WeightedGrade g : grades) {
            BigDecimal coeff = g.coefficient() != null ? g.coefficient() : BigDecimal.ONE;
            num = num.add(g.value().multiply(coeff, MC));
            den = den.add(coeff);
        }
        return den.signum() == 0 ? BigDecimal.ZERO : num.divide(den, MC);
    }

    private static BigDecimal best(List<WeightedGrade> grades) {
        return grades.stream().map(WeightedGrade::value).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    private static BigDecimal dropLowest(List<WeightedGrade> grades) {
        if (grades.size() <= 1) {
            return weighted(grades);
        }
        WeightedGrade lowest = grades.stream()
                .min(Comparator.comparing(WeightedGrade::value)).orElseThrow();
        List<WeightedGrade> kept = new java.util.ArrayList<>(grades);
        kept.remove(lowest); // retire une seule occurrence de la note la plus faible
        return weighted(kept);
    }

    private static BigDecimal round(BigDecimal value, int decimals, RoundingPolicy rounding) {
        int scale = Math.max(0, decimals);
        RoundingPolicy policy = rounding != null ? rounding : RoundingPolicy.MATHEMATICAL;
        return value.setScale(scale, policy.toRoundingMode());
    }
}
