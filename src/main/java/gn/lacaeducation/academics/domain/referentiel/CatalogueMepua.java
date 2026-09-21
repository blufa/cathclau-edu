package gn.lacaeducation.academics.domain.referentiel;

import gn.lacaeducation.academics.domain.valueobject.Grade;
import gn.lacaeducation.academics.domain.valueobject.SchoolCycle;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cartographie officielle des matières scolaires et de leurs coefficients selon les normes MEPUA
 * (Guinée). Sert de dictionnaire de référence pour la programmation annuelle du Directeur des
 * Études : une école peut importer ces matières/coefficients pour un cycle (et une série au
 * lycée) plutôt que de les ressaisir manuellement.
 *
 * <p>Au lycée, la série "LITTERAIRE" ou "SCIENTIFIQUE" détermine le programme (11ème à Terminale) —
 * les coefficients par matière restent modifiables ensuite par l'école (Référentiels → Matières).
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

    /**
     * Résout le catalogue MEPUA d'une classe à partir de son {@link Grade} — dérive Collège vs Lycée
     * du grade plutôt que du Niveau, qui ne distingue plus les deux depuis la réforme du catalogue de
     * référentiels (réduit à Maternelle/Primaire/Secondaire).
     */
    public static List<MatiereModele> pourClasse(Grade grade, String serie) {
        if (grade == null) return List.of();
        return pour(grade.mepuaCycle(), serie);
    }

    private static List<MatiereModele> lycee(String serie) {
        if (serie == null || serie.isBlank()) return List.of();
        return switch (serie.trim().toUpperCase()) {
            case "LITTERAIRE" -> LITTERAIRE;
            case "SCIENTIFIQUE" -> SCIENTIFIQUE;
            default -> List.of();
        };
    }

    private static BigDecimal c(int value) {
        return BigDecimal.valueOf(value);
    }

    /** Primaire (CP1 → CM2) : tronc commun, coefficient uniforme (1). */
    public static final List<MatiereModele> PRIMAIRE = List.of(
            new MatiereModele("REDAC", "Français (Rédaction / Composition)", c(1)),
            new MatiereModele("DICT_Q", "Français (Dictée - Questions)", c(1)),
            new MatiereModele("CALC_E", "Calcul Écrit", c(1)),
            new MatiereModele("CALC_M", "Calcul Mental", c(1)),
            new MatiereModele("HIST", "Histoire", c(1)),
            new MatiereModele("GEO", "Géographie", c(1)),
            new MatiereModele("SCI_OBS", "Sciences d'Observation", c(1)),
            new MatiereModele("ECM", "Éducation Civique et Morale", c(1)),
            new MatiereModele("DESSIN", "Dessin / Travail Manuel", c(1)),
            new MatiereModele("EPS", "Éducation Physique et Sportive", c(1))
    );

    /** Collège (7ème → 10ème) : Mathématiques, Physique, Chimie, Biologie, Français (Dictée-Questions
     * + Rédaction, deux matières notées séparément pour le même cours), Histoire, Géographie, ECM,
     * EPS, Anglais. */
    public static final List<MatiereModele> COLLEGE = List.of(
            new MatiereModele("REDAC", "Français (Rédaction)", c(2)),
            new MatiereModele("DICT_Q", "Français (Dictée - Questions)", c(2)),
            new MatiereModele("MATH", "Mathématiques", c(4)),
            new MatiereModele("PHYS", "Physique", c(1)),
            new MatiereModele("CHIM", "Chimie", c(1)),
            new MatiereModele("BIO", "Biologie", c(2)),
            new MatiereModele("HIST", "Histoire", c(1)),
            new MatiereModele("GEO", "Géographie", c(1)),
            new MatiereModele("ECM", "Éducation Civique et Morale", c(1)),
            new MatiereModele("ANG", "Anglais", c(2)),
            new MatiereModele("EPS", "Éducation Physique et Sportive", c(1))
    );

    /** Lycée, Série Littéraire (11ème à Terminale) : Français, Géographie, Histoire, Philosophie,
     * Économie, Mathématiques, Physique, Chimie, Anglais. */
    public static final List<MatiereModele> LITTERAIRE = List.of(
            new MatiereModele("FRAN", "Français", c(4)),
            new MatiereModele("PHILO", "Philosophie", c(3)),
            new MatiereModele("HIST", "Histoire", c(2)),
            new MatiereModele("GEO", "Géographie", c(2)),
            new MatiereModele("ECON", "Économie", c(2)),
            new MatiereModele("ANG", "Anglais", c(2)),
            new MatiereModele("MATH", "Mathématiques", c(1)),
            new MatiereModele("PHYS", "Physique", c(1)),
            new MatiereModele("CHIM", "Chimie", c(1))
    );

    /** Lycée, Série Scientifique (11ème à Terminale) : Mathématiques, Physique, Chimie, Français,
     * Anglais, Économie-Politique, Biologie, Géologie, Histoire, Géographie. */
    public static final List<MatiereModele> SCIENTIFIQUE = List.of(
            new MatiereModele("MATH", "Mathématiques", c(4)),
            new MatiereModele("PHYS", "Physique", c(3)),
            new MatiereModele("CHIM", "Chimie", c(3)),
            new MatiereModele("BIO", "Biologie", c(2)),
            new MatiereModele("GEOL", "Géologie", c(1)),
            new MatiereModele("FRAN", "Français", c(2)),
            new MatiereModele("ANG", "Anglais", c(2)),
            new MatiereModele("ECON_POL", "Économie-Politique", c(1)),
            new MatiereModele("HIST", "Histoire", c(1)),
            new MatiereModele("GEO", "Géographie", c(1))
    );
}
