package gn.lacaeducation.grading.domain;

/**
 * Étiquette optionnelle d'une {@link GradingFormulaStep} indiquant quelle colonne du bulletin son
 * résultat alimente. Permet à {@link GradeCalculationService} de calculer la moyenne générale de
 * l'élève (toutes matières confondues) en traitant séparément la moyenne de cours (simple, non
 * pondérée par coefficient) et la moyenne de composition (pondérée par coefficient), avant de les
 * combiner — méthode de calcul officielle distincte du calcul plat historique.
 */
public enum StepRole {
    /** Étape sans rôle particulier (comportement historique, non exploitée pour la moyenne générale). */
    NONE,
    /** Étape représentant la moyenne de cours (contrôle continu) de la matière. */
    COURS,
    /** Étape représentant la moyenne de composition (examen) de la matière. */
    COMPOSITION
}
