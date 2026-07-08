package gn.lacaeducation.grading.domain;

/** Type of change recorded in the grade modification history (traçabilité des notes). */
public enum GradeAction {
    /** Première saisie de la note. */
    ENTERED,
    /** Correction de la valeur de la note. */
    UPDATED,
    /** Note validée (verrouillée). */
    VALIDATED,
    /** Note déverrouillée pour correction. */
    UNLOCKED,
    /** Note supprimée. */
    DELETED
}
