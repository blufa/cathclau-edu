package gn.lacaeducation.grading.domain;

/** Poids appliqué à une entrée d'une étape {@link StepKind#COMBINE} de {@link GradingFormula}. */
public enum WeightMode {
    /** Poids 1 pour cette entrée. */
    EQUAL,
    /** Poids = coefficient de la matière (ex. la composition pèse autant que le coefficient de la matière). */
    SUBJECT_COEFFICIENT,
    /** Poids fixe saisi ({@link GradingFormulaStepInput#getFixedWeight()}). */
    FIXED
}
