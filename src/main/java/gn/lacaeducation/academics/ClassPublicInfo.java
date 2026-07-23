package gn.lacaeducation.academics;
/** classType : "NORMALE" ou "EXAMEN" (§6 paie — tarif horaire de l'enseignant qui y intervient). */
public record ClassPublicInfo(Long id, String label, String levelLabel, String classType) {}
