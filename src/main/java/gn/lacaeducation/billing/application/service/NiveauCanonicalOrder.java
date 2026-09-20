package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort.ActiveLevel;

import java.text.Normalizer;
import java.util.List;

/**
 * Progression pédagogique officielle guinéenne (garderie → petite/moyenne/grande section → CP1…CM2 →
 * 7e…10e → 11e…terminale), utilisée pour départager le cadet d'une fratrie (§5, remise fratrie
 * nombreuse) indépendamment du {@code sort_order} que chaque école saisit elle-même à la main pour
 * ses niveaux — un sort_order mal renseigné (ex. grande section classée avant moyenne section) ne doit
 * jamais faire ressortir plusieurs « cadets » dans une même fratrie. Le cycle (garderie/maternelle/
 * primaire/collège/lycée) donne le classement grossier ; au sein d'un même cycle, le libellé du niveau
 * (normalisé, insensible aux accents/casse) donne le classement fin.
 */
final class NiveauCanonicalOrder {

    private NiveauCanonicalOrder() {
    }

    private static final List<List<String>> MATERNELLE = List.of(
            List.of("petite", "ps"),
            List.of("moyenne", "ms"),
            List.of("grande", "gs"));
    private static final List<List<String>> PRIMAIRE = List.of(
            List.of("cp1"), List.of("cp2"), List.of("ce1"), List.of("ce2"), List.of("cm1"), List.of("cm2"));
    private static final List<List<String>> COLLEGE = List.of(
            List.of("7e"), List.of("8e"), List.of("9e"), List.of("10e"));
    private static final List<List<String>> LYCEE = List.of(
            List.of("11e"), List.of("12e"), List.of("terminale", "tle"));

    // Reprend l'ordre de gn.lacaeducation.academics.domain.valueobject.SchoolCycle — dupliqué ici (pas
    // d'import direct) car ce type interne au module academics n'est pas exposé aux autres modules
    // (frontière Spring Modulith) ; seul l'ordre des noms de cycle nous intéresse ici.
    private static final List<String> CYCLES = List.of("GARDERIE", "MATERNELLE", "PRIMAIRE", "COLLEGE", "LYCEE", "SUPERIEUR");

    /** Rang du cycle (0 = le plus bas, garderie), ou {@code null} si le cycle est inconnu/absent. */
    static Integer cycleRank(String cycle) {
        if (cycle == null) return null;
        int i = CYCLES.indexOf(cycle);
        return i < 0 ? null : i;
    }

    /**
     * Rang au sein du cycle : le libellé canonique reconnu (0..n) prime toujours sur le sort_order
     * saisi par l'école ; à défaut de libellé reconnu, replie sur {@code 1000 + sortOrder} (classé
     * après tout niveau reconnu du même cycle) ; {@code null} si ni l'un ni l'autre n'est disponible.
     */
    static Integer effectiveWithinCycleRank(ActiveLevel level) {
        if (level == null) return null;
        Integer canonical = withinCycleRank(level.cycle(), level.label());
        if (canonical != null) return canonical;
        return level.sortOrder() != null ? 1000 + level.sortOrder() : null;
    }

    private static Integer withinCycleRank(String cycle, String label) {
        if (cycle == null || label == null) return null;
        List<List<String>> tokens = switch (cycle) {
            case "MATERNELLE" -> MATERNELLE;
            case "PRIMAIRE" -> PRIMAIRE;
            case "COLLEGE" -> COLLEGE;
            case "LYCEE" -> LYCEE;
            default -> null; // GARDERIE (niveau unique) et SUPERIEUR : pas de sous-progression connue
        };
        if (tokens == null) return null;
        String normalized = normalize(label);
        for (int i = 0; i < tokens.size(); i++) {
            for (String token : tokens.get(i)) {
                if (normalized.contains(token)) return i;
            }
        }
        return null;
    }

    private static String normalize(String s) {
        String noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
