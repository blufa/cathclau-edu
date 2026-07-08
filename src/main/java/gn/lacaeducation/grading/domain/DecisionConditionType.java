package gn.lacaeducation.grading.domain;

/** Type de condition d'une règle de décision (§17), sans code : conditions structurées. */
public enum DecisionConditionType {
    /** Moyenne générale ≥ seuil. */
    OVERALL_AT_LEAST,
    /** Moyenne générale &lt; seuil. */
    OVERALL_BELOW,
    /** Moyenne d'une matière donnée &lt; seuil. */
    SUBJECT_BELOW,
    /** Nombre de matières dont la moyenne est &lt; seuil ≥ un minimum. */
    SUBJECTS_BELOW_COUNT
}
