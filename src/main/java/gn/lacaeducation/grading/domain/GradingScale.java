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
    private CycleGroup cycleGroup;
    private BigDecimal passThreshold;
    private BigDecimal passableMin;
    private BigDecimal assezBienMin;
    private BigDecimal bienMin;
    private BigDecimal tresBienMin;
    /** Moyenne générale minimale pour les Félicitations du conseil de classe. */
    private BigDecimal felicitationsMin;
    /** Moyenne générale minimale pour les Compliments du conseil de classe. */
    private BigDecimal complimentsMin;
    /** Aucune moyenne de matière sous ce seuil pour prétendre à une distinction automatique. */
    private BigDecimal distinctionNoFailingThreshold;

    public static GradingScale defaults() {
        return GradingScale.builder()
                .passThreshold(BigDecimal.valueOf(10))
                .passableMin(BigDecimal.valueOf(10))
                .assezBienMin(BigDecimal.valueOf(14))
                .bienMin(BigDecimal.valueOf(16))
                .tresBienMin(BigDecimal.valueOf(18))
                .felicitationsMin(BigDecimal.valueOf(16))
                .complimentsMin(BigDecimal.valueOf(13))
                .distinctionNoFailingThreshold(BigDecimal.valueOf(10))
                .build();
    }

    /** Barème par défaut rattaché à un groupe de cycle donné (primaire/secondaire). */
    public static GradingScale defaults(CycleGroup cycleGroup) {
        GradingScale scale = defaults();
        scale.setCycleGroup(cycleGroup);
        return scale;
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

    /**
     * Distinction automatique du conseil de classe selon le barème du tenant : Félicitations si
     * {@code generalAverage >= felicitationsMin}, Compliments si {@code >= complimentsMin}, à condition
     * qu'aucune moyenne de matière ne soit sous {@code distinctionNoFailingThreshold}. Les autres
     * distinctions (Encouragements, Mise en garde, Tableau d'honneur) restent à l'appréciation manuelle.
     *
     * @param minSubjectAverage la plus faible moyenne de matière de l'élève (peut être {@code null})
     */
    public Distinction distinctionFor(BigDecimal generalAverage, BigDecimal minSubjectAverage) {
        if (generalAverage == null) return Distinction.NONE;
        BigDecimal felicMin = felicitationsMin != null ? felicitationsMin : BigDecimal.valueOf(16);
        BigDecimal complMin = complimentsMin != null ? complimentsMin : BigDecimal.valueOf(13);
        BigDecimal noFailing = distinctionNoFailingThreshold != null ? distinctionNoFailingThreshold : BigDecimal.TEN;
        boolean allPass = minSubjectAverage == null || minSubjectAverage.compareTo(noFailing) >= 0;
        if (!allPass) return Distinction.NONE;
        if (generalAverage.compareTo(felicMin) >= 0) return Distinction.FELICITATIONS;
        if (generalAverage.compareTo(complMin) >= 0) return Distinction.COMPLIMENTS;
        return Distinction.NONE;
    }
}
