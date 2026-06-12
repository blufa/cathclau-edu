package gn.lacaeducation.grading.domain;

import lombok.*;
import java.math.BigDecimal;

/**
 * Per-tenant grading scale: pass threshold (admission decision) and the lower bounds of each honor
 * (mention) band, all expressed out of 20. {@link #defaults()} provides the standard Guinean scale.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GradingScale {
    private Long id;
    private Long tenantId;
    private BigDecimal passThreshold;
    private BigDecimal passableMin;
    private BigDecimal assezBienMin;
    private BigDecimal bienMin;
    private BigDecimal tresBienMin;

    public static GradingScale defaults() {
        return GradingScale.builder()
                .passThreshold(BigDecimal.valueOf(10))
                .passableMin(BigDecimal.valueOf(10))
                .assezBienMin(BigDecimal.valueOf(14))
                .bienMin(BigDecimal.valueOf(16))
                .tresBienMin(BigDecimal.valueOf(18))
                .build();
    }

    public String honorFor(BigDecimal average) {
        if (average == null) return "Insuffisant";
        if (average.compareTo(tresBienMin) >= 0) return "Très Bien";
        if (average.compareTo(bienMin) >= 0) return "Bien";
        if (average.compareTo(assezBienMin) >= 0) return "Assez Bien";
        if (average.compareTo(passableMin) >= 0) return "Passable";
        return "Insuffisant";
    }

    public String decisionFor(BigDecimal average) {
        if (average == null) return "REDOUBLE";
        return average.compareTo(passThreshold) >= 0 ? "ADMIS" : "REDOUBLE";
    }
}
