package gn.lacaeducation.grading.domain;

import lombok.*;

/**
 * Réglage par établissement du moteur de calcul des moyennes (§2 + §5) : méthode de calcul de la
 * moyenne matière, nombre de décimales et politique d'arrondi. {@link #defaults()} reproduit le
 * comportement historique (moyenne pondérée, 2 décimales, arrondi mathématique).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradingCalculationSettings {
    private Long id;
    private Long tenantId;
    private AverageMethod averageMethod;
    private Integer decimals;
    private RoundingPolicy rounding;

    public static GradingCalculationSettings defaults() {
        return GradingCalculationSettings.builder()
                .averageMethod(AverageMethod.WEIGHTED)
                .decimals(2)
                .rounding(RoundingPolicy.MATHEMATICAL)
                .build();
    }

    public int decimalsOrDefault() {
        return decimals != null && decimals >= 0 ? decimals : 2;
    }

    public AverageMethod methodOrDefault() {
        return averageMethod != null ? averageMethod : AverageMethod.WEIGHTED;
    }

    public RoundingPolicy roundingOrDefault() {
        return rounding != null ? rounding : RoundingPolicy.MATHEMATICAL;
    }
}
