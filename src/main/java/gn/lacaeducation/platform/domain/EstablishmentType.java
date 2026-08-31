package gn.lacaeducation.platform.domain;

/** Kind of school registered as a tenant (features/01-platform.md §1, signup payload).
 * MATERNELLE regroupe les niveaux Petite Section, Moyenne Section et Grande Section. */
public enum EstablishmentType {
    MATERNELLE, PRIMAIRE, COLLEGE, LYCEE, SUPERIEUR, MIXTE
}
