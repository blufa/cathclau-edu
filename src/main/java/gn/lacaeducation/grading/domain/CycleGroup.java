package gn.lacaeducation.grading.domain;

/**
 * Regroupement des cycles scolaires ({@code gn.lacaeducation.academics.domain.valueobject.SchoolCycle})
 * en deux familles de barème : le primaire (Garderie/Maternelle/Primaire) et le secondaire
 * (Collège/Lycée/Supérieur). Même découpage que {@code SchoolCycle.defaultPeriodType()}
 * (primaire = trimestres, reste = semestres), réutilisé ici pour rester cohérent.
 */
public enum CycleGroup {
    PRIMAIRE, SECONDAIRE;

    /** @param schoolCycle la valeur brute de {@code SchoolCycle} (ex. "PRIMAIRE", "COLLEGE"...). */
    public static CycleGroup fromSchoolCycle(String schoolCycle) {
        if (schoolCycle == null) return SECONDAIRE;
        return switch (schoolCycle) {
            case "GARDERIE", "MATERNELLE", "PRIMAIRE" -> PRIMAIRE;
            default -> SECONDAIRE;
        };
    }
}
