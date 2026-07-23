package gn.lacaeducation.grading.domain;

/** Nature d'une étape de {@link GradingFormula} : agrégation de notes brutes, ou combinaison de résultats d'étapes précédentes. */
public enum StepKind {
    /** Calcule une moyenne à partir des évaluations de la période filtrées par mois ou par type d'évaluation (ex. « Moyenne d'octobre », « Moyenne de composition »). */
    GROUP,
    /** Calcule une moyenne pondérée des résultats d'étapes précédentes (ex. « Moyenne de cours », « Moyenne semestrielle »). */
    COMBINE
}
