package gn.lacaeducation.platform.domain;

/** Kind of school registered as a tenant (features/01-platform.md §1, signup payload).
 * MATERNELLE regroupe les niveaux Petite Section, Moyenne Section et Grande Section. GARDERIE est un
 * cycle à part (crèche/pré-maternelle), distinct de MATERNELLE — sans lui dans la liste déclarée par
 * l'école, aucune classe de ce cycle ne peut être créée (§academics). */
public enum EstablishmentType {
    GARDERIE, MATERNELLE, PRIMAIRE, COLLEGE, LYCEE, SUPERIEUR, MIXTE
}
