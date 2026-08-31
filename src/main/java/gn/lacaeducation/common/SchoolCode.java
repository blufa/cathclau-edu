package gn.lacaeducation.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Construit le sigle de l'école utilisé en préfixe de toutes les nomenclatures (matricules, cartes…).
 * Le sigle est l'acronyme des mots significatifs et distinctifs du nom : articles, prépositions et
 * désignations génériques d'établissement (« Groupe », « Scolaire », « École »…) sont ignorés, pour ne
 * garder que ce qui distingue vraiment l'école — « Groupe Scolaire Al Taqwa » → « AT », « École
 * Primaire Les Anges » → « PA ». Pour un nom d'un seul mot significatif, on retient ses 3 premières
 * lettres (« Renaissance » → « REN »).
 */
public final class SchoolCode {

    private SchoolCode() {}

    /** Mots vides ignorés dans la construction du sigle : articles/prépositions (FR + EN) et les
     * désignations génériques d'établissement, qui ne distinguent pas une école d'une autre. */
    private static final Set<String> STOP_WORDS = Set.of(
            "de", "du", "des", "la", "le", "les", "l", "et", "d", "a", "au", "aux", "en",
            "the", "of", "and",
            "groupe", "scolaire", "ecole", "ecoles", "college", "lycee", "institut", "complexe");

    /** Sigle (2 à 6 lettres majuscules) dérivé du nom de l'école ; « EC » par défaut si indisponible. */
    public static String prefix(String schoolName) {
        if (schoolName == null || schoolName.isBlank()) return "EC";
        String ascii = Normalizer.normalize(schoolName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String[] words = ascii.trim().split("[^A-Za-z0-9]+");
        StringBuilder acronym = new StringBuilder();
        String firstSignificant = null;
        for (String w : words) {
            if (w.isEmpty() || STOP_WORDS.contains(w.toLowerCase(Locale.ROOT))) continue;
            if (firstSignificant == null) firstSignificant = w;
            acronym.append(Character.toUpperCase(w.charAt(0)));
        }
        String sigle = acronym.toString();
        if (sigle.length() >= 2) {
            return sigle.length() > 6 ? sigle.substring(0, 6) : sigle;
        }
        // Un seul mot significatif : on retient ses premières lettres (ex. « Renaissance » → « REN »).
        String base = (firstSignificant != null ? firstSignificant : ascii).replaceAll("[^A-Za-z]", "");
        if (base.length() < 2) base = base + "EC";
        return base.substring(0, Math.min(3, base.length())).toUpperCase(Locale.ROOT);
    }

    /** Résout le préfixe école pour un tenant via {@link TenantInfoProvider}. */
    public static String prefix(TenantInfoProvider provider, Long tenantId) {
        return prefix(Optional.ofNullable(tenantId)
                .flatMap(provider::findTenantInfo)
                .map(TenantPublicInfo::schoolName)
                .orElse(null));
    }
}
