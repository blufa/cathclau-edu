package gn.lacaeducation.academics;
/** classType : "NORMALE" ou "EXAMEN" (§6 paie — tarif horaire de l'enseignant qui y intervient).
 * classRepresentativeStudentId : élève désigné pour scanner les badges de la classe (§attendance).
 * headTeacherId : professeur principal — résout l'enseignant d'une séance sans matière précise
 * (moniteur maternelle/primaire, §lessons).
 * schoolCycle : cycle du niveau de la classe ("MATERNELLE"/"PRIMAIRE"/"COLLEGE"/"LYCEE"/"SUPERIEUR"),
 * null si non renseigné — déclenche le badge de retrait automatique à la sortie (§attendance). */
public record ClassPublicInfo(Long id, String label, String levelLabel, String classType,
                              Long classRepresentativeStudentId, Long headTeacherId, String schoolCycle) {}
