package gn.lacaeducation.students;

/** Une inscription saisie un jour précis (§retour comptable, "ce jour tu inscris tant d'élèves en
 * classe") — {@code classId} résolu en nom par l'appelant (voir AcademicsDataProvider), jamais
 * d'identifiant brut affiché côté écran. */
public record EnrollmentDayInfo(Long studentId, String studentName, String studentNumber,
                                Long classId, EnrollmentType type, String status) {}
