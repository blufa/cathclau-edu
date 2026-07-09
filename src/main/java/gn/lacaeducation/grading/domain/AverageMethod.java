package gn.lacaeducation.grading.domain;

/** Méthode de calcul de la moyenne d'une matière (§5), paramétrable par établissement. */
public enum AverageMethod {
    /** Moyenne simple : Σ notes / nombre de notes. */
    SIMPLE,
    /** Moyenne pondérée : Σ(note × coefficient d'évaluation) / Σ(coefficients). */
    WEIGHTED,
    /** Meilleure note conservée. */
    BEST_SCORE,
    /** Moyenne pondérée après suppression de la note la plus faible. */
    DROP_LOWEST,
    /**
     * Moyenne par catégorie : moyenne de chaque catégorie (type d'évaluation) pondérée par son poids
     * (ex. Contrôle continu 40 % + Composition 60 %). Nécessite les poids des catégories.
     */
    BY_CATEGORY
}
