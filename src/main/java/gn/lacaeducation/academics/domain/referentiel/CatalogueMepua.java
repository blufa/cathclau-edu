package gn.lacaeducation.academics.domain.referentiel;

import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cartographie officielle des matières scolaires et de leurs coefficients selon les normes MEPUA
 * (Guinée). Sert de dictionnaire de référence pour la programmation annuelle du Directeur des
 * Études : une école peut importer ces matières/coefficients pour un cycle (et une série au
 * lycée) plutôt que de les ressaisir manuellement.
 *
 * <p>Au lycée, la série "SCIENCES" / "SOCIALES" correspond au tronc commun de la 11ème année ;
 * "SM" / "SE" / "SS" correspondent aux séries du Baccalauréat unique (12ème et Terminale).
 */
public final class CatalogueMepua {

    private CatalogueMepua() {
    }

    /** Résout la liste de matières/coefficients pour un cycle donné (et une série pour le lycée). */
    public static List<MatiereModele> pour(SchoolCycle cycle, String serie) {
        return switch (cycle) {
            case GARDERIE, MATERNELLE -> List.of(); // pas de catalogue MEPUA officiel pour la garderie/maternelle
            case PRIMAIRE -> PRIMAIRE;
            case COLLEGE -> COLLEGE;
            case LYCEE -> lycee(serie);
            case SUPERIEUR -> List.of();
        };
    }

    private static List<MatiereModele> lycee(String serie) {
        if (serie == null || serie.isBlank()) return List.of();
        return switch (serie.trim().toUpperCase()) {
            case "SCIENCES" -> ONZIEME_SCIENCES;
            case "SOCIALES" -> ONZIEME_SOCIALES;
            case "SM" -> TERMINALE_SM;
            case "SE" -> TERMINALE_SE;
            case "SS" -> TERMINALE_SS;
            default -> List.of();
        };
    }

    private static BigDecimal c(int value) {
        return BigDecimal.valueOf(value);
    }

    /** Primaire (CP1 → CM2) : tronc commun, coefficient uniforme (1). */
    public static final List<MatiereModele> PRIMAIRE = List.of(
            new MatiereModele("REDAC", "Rédaction / Composition", c(1)),
            new MatiereModele("DICT_Q", "Dictée - Questions", c(1)),
            new MatiereModele("CALC_E", "Calcul Écrit", c(1)),
            new MatiereModele("CALC_M", "Calcul Mental", c(1)),
            new MatiereModele("HIST", "Histoire", c(1)),
            new MatiereModele("GEO", "Géographie", c(1)),
            new MatiereModele("SCI_OBS", "Sciences d'Observation", c(1)),
            new MatiereModele("ECM", "Éducation Civique et Morale", c(1)),
            new MatiereModele("DESSIN", "Dessin / Travail Manuel", c(1)),
            new MatiereModele("EPS", "Éducation Physique et Sportive", c(1))
    );

    /** Collège (7ème → 10ème) : coefficients officiels BEPC / moyennes. */
    public static final List<MatiereModele> COLLEGE = List.of(
            new MatiereModele("REDAC", "Rédaction", c(2)),
            new MatiereModele("DICT_Q", "Dictée - Questions", c(2)),
            new MatiereModele("MATH", "Mathématiques", c(4)),
            new MatiereModele("PHYS", "Sciences Physique", c(2)),
            new MatiereModele("SVT", "Sciences de la Vie et de la Terre", c(2)),
            new MatiereModele("HIST", "Histoire", c(1)),
            new MatiereModele("GEO", "Géographie", c(1)),
            new MatiereModele("ECM", "Éducation Civique et Morale", c(1)),
            new MatiereModele("ANG", "Anglais", c(2)),
            new MatiereModele("EPS", "Éducation Physique et Sportive", c(1))
    );

    /** 11ème année, profil Sciences (futurs SM / SE) : tronc commun de transition. */
    public static final List<MatiereModele> ONZIEME_SCIENCES = List.of(
            new MatiereModele("MATH", "Mathématiques", c(4)),
            new MatiereModele("PHYS", "Physique", c(3)),
            new MatiereModele("CHIM", "Chimie", c(2)),
            new MatiereModele("SVT", "Sciences de la Vie et de la Terre", c(2)),
            new MatiereModele("FRAN", "Français", c(2)),
            new MatiereModele("ANG", "Anglais", c(2)),
            new MatiereModele("HIST_GEO", "Histoire-Géographie", c(1)),
            new MatiereModele("EPS", "Éducation Physique et Sportive", c(1))
    );

    /** 11ème année, profil Sciences Sociales (futurs SS) : tronc commun de transition. */
    public static final List<MatiereModele> ONZIEME_SOCIALES = List.of(
            new MatiereModele("HIST", "Histoire", c(3)),
            new MatiereModele("GEO", "Géographie", c(3)),
            new MatiereModele("FRAN_LITT", "Français / Littérature", c(3)),
            new MatiereModele("ECM", "Éducation Civique et Morale", c(2)),
            new MatiereModele("ANG", "Anglais", c(2)),
            new MatiereModele("MATH_APP", "Mathématiques Appliquées", c(2)),
            new MatiereModele("PHILO", "Philosophie (Intro)", c(1)),
            new MatiereModele("EPS", "Éducation Physique et Sportive", c(1))
    );

    /** 12ème / Terminale, série Sciences Mathématiques (SM). */
    public static final List<MatiereModele> TERMINALE_SM = List.of(
            new MatiereModele("MATH", "Mathématiques", c(5)),
            new MatiereModele("PHYS", "Physique", c(4)),
            new MatiereModele("CHIM", "Chimie", c(3)),
            new MatiereModele("SVT", "SVT (Biologie)", c(1)),
            new MatiereModele("FRAN", "Français / Littérature", c(2)),
            new MatiereModele("PHILO", "Philosophie", c(2)),
            new MatiereModele("ANG", "Anglais", c(2))
    );

    /** 12ème / Terminale, série Sciences Expérimentales (SE). */
    public static final List<MatiereModele> TERMINALE_SE = List.of(
            new MatiereModele("MATH", "Mathématiques", c(3)),
            new MatiereModele("PHYS", "Physique", c(3)),
            new MatiereModele("CHIM", "Chimie", c(3)),
            new MatiereModele("SVT", "SVT (Biologie)", c(5)),
            new MatiereModele("FRAN", "Français / Littérature", c(2)),
            new MatiereModele("PHILO", "Philosophie", c(2)),
            new MatiereModele("ANG", "Anglais", c(2))
    );

    /** 12ème / Terminale, série Sciences Sociales (SS). */
    public static final List<MatiereModele> TERMINALE_SS = List.of(
            new MatiereModele("MATH", "Mathématiques", c(2)),
            new MatiereModele("FRAN", "Français / Littérature", c(3)),
            new MatiereModele("PHILO", "Philosophie", c(4)),
            new MatiereModele("HIST", "Histoire", c(3)),
            new MatiereModele("GEO", "Géographie", c(3)),
            new MatiereModele("ECON_ECM", "Économie / ECM", c(2)),
            new MatiereModele("ANG", "Anglais", c(2))
    );
}
