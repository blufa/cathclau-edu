package gn.lacaeducation.students.domain;

/** Comment l'élève est arrivé dans l'établissement (§retour fondateur, suivi façon plateforme de
 * l'État) — purement informatif/UX, distinct de {@code EnrollmentType} (NEW/RE_ENROLLMENT) qui
 * pilote la sélection du frais de facturation et ne doit pas être touché par cette distinction.
 * TRANSFERT exige que {@code Student.originSchool} et {@code Student.transferReason} soient
 * renseignés. */
public enum AdmissionType { NOUVELLE, TRANSFERT }
