package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort.ActiveGrade;

import java.util.List;

/**
 * Progression pédagogique officielle guinéenne (garderie → petite/moyenne/grande section → CP1…CM2 →
 * 7e…10e → 11e…terminale), utilisée pour départager le cadet d'une fratrie (§5, remise fratrie
 * nombreuse). Le rang fin vient du {@code grade} de la classe de l'élève (garanti fiable, dérivé d'un
 * enum plat côté academics) ; à défaut de grade renseigné (classe historique non migrée ou
 * nomenclature maison hors catalogue), replie sur le cycle grossier du Niveau (Maternelle/Primaire/
 * Secondaire depuis la réforme du catalogue de référentiels) × son sort_order.
 *
 * <p>Reprend l'ordre EXACT de {@code gn.lacaeducation.academics.domain.valueobject.Grade} — dupliqué
 * ici (pas d'import direct : ce type interne au module academics n'est pas exposé aux autres modules,
 * frontière Spring Modulith) sous forme de la liste plate des noms de constantes. NE PAS réordonner
 * sans garder les deux synchronisés.
 */
final class NiveauCanonicalOrder {

    private NiveauCanonicalOrder() {
    }

    private static final List<String> GRADES = List.of(
            "GARDERIE",
            "PETITE_SECTION", "MOYENNE_SECTION", "GRANDE_SECTION",
            "CP1", "CP2", "CE1", "CE2", "CM1", "CM2",
            "SIXIEME", "CINQUIEME", "QUATRIEME", "TROISIEME",
            "SEPTIEME", "HUITIEME", "NEUVIEME", "DIXIEME",
            "SECONDE", "PREMIERE",
            "ONZIEME", "DOUZIEME", "TERMINALE");

    // Le Niveau (réduit à 3 valeurs) stocke toujours cycle=MATERNELLE/PRIMAIRE/COLLEGE (COLLEGE
    // représentant "Secondaire" par convention, voir migration V194) — repli grossier uniquement.
    private static final List<String> NIVEAU_CYCLES = List.of("MATERNELLE", "PRIMAIRE", "COLLEGE");

    /** Rang fin (0..n) du grade reconnu, ou {@code null} si absent/non reconnu. */
    static Integer gradeRank(String grade) {
        if (grade == null) return null;
        int i = GRADES.indexOf(grade);
        return i < 0 ? null : i;
    }

    /** Rang grossier de repli (Niveau à 3 valeurs), utilisé uniquement quand le grade de la classe
     * est absent/non reconnu — départage au moins Maternelle &lt; Primaire &lt; Secondaire, avec le
     * sort_order de l'école pour affiner au sein du niveau si fourni. Toujours classé strictement
     * après tout grade fin reconnu (offset {@code 1000+}). */
    static Integer niveauFallbackRank(String niveauCycle, Integer sortOrder) {
        if (niveauCycle == null) return null;
        int i = NIVEAU_CYCLES.indexOf(niveauCycle);
        if (i < 0) return null;
        return 1000 + i * 1000 + (sortOrder != null ? sortOrder : 0);
    }

    /** Rang effectif : grade fin de la classe en priorité, repli sur le niveau grossier sinon. */
    static Integer effectiveRank(ActiveGrade level) {
        if (level == null) return null;
        Integer fine = gradeRank(level.grade());
        if (fine != null) return fine;
        return niveauFallbackRank(level.niveauCycle(), level.niveauSortOrder());
    }
}
