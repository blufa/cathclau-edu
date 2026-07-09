package gn.lacaeducation.students;

/**
 * Type d'inscription. Exposé au niveau racine du module (API publique) car il fait partie du contrat
 * de {@code EnrollmentCreatedEvent} consommé par d'autres modules (billing).
 */
public enum EnrollmentType { NEW, RE_ENROLLMENT }
