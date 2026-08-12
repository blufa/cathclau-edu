package gn.lacaeducation.grading.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Réglage par établissement du moteur de calcul des moyennes (§2 + §5) : méthode de calcul de la
 * moyenne matière, nombre de décimales et politique d'arrondi. {@link #defaults()} reproduit le
 * comportement historique (moyenne pondérée, 2 décimales, arrondi mathématique).
 * <p>
 * {@link #generalCoursWeight}/{@link #generalCompoWeight} pondèrent la moyenne générale de l'élève
 * (toutes matières confondues) quand des étapes de formule sont étiquetées {@code COURS}/{@code
 * COMPOSITION} (§StepRole) : par défaut la composition compte double (2) par rapport au cours (1),
 * méthode de calcul officielle confirmée sur les documents du Ministère.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradingCalculationSettings {
    private Long id;
    private Long tenantId;
    private AverageMethod averageMethod;
    private Integer decimals;
    private RoundingPolicy rounding;
    private BigDecimal generalCoursWeight;
    private BigDecimal generalCompoWeight;

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

    public BigDecimal generalCoursWeightOrDefault() {
        return generalCoursWeight != null ? generalCoursWeight : BigDecimal.ONE;
    }

    public BigDecimal generalCompoWeightOrDefault() {
        return generalCompoWeight != null ? generalCompoWeight : BigDecimal.valueOf(2);
    }
}
