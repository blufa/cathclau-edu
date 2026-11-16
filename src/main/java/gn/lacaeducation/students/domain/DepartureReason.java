package gn.lacaeducation.students.domain;

/** Raison du départ d'un élève en cours d'année (§retour fondateur, suivi façon plateforme de
 * l'État) — purement informatif, ne change rien au comportement déclenché par
 * {@link EnrollmentStatus#ABANDONED} (listes actives, facturation), identique quelle que soit la
 * raison. */
public enum DepartureReason { TRANSFERT, ABANDON, EXCLUSION, AUTRE }
