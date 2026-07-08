package gn.lacaeducation.grading.domain;

import java.math.RoundingMode;

/** Politique d'arrondi des moyennes (§2), paramétrable par établissement. */
public enum RoundingPolicy {
    /** Arrondi mathématique (0,5 arrondi au supérieur). */
    MATHEMATICAL(RoundingMode.HALF_UP),
    /** Arrondi supérieur systématique. */
    UP(RoundingMode.CEILING),
    /** Arrondi inférieur systématique. */
    DOWN(RoundingMode.FLOOR),
    /** Aucun arrondi : troncature au nombre de décimales. */
    NONE(RoundingMode.DOWN);

    private final RoundingMode mode;

    RoundingPolicy(RoundingMode mode) {
        this.mode = mode;
    }

    public RoundingMode toRoundingMode() {
        return mode;
    }
}
