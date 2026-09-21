package gn.lacaeducation.common;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalise la casse des noms/prénoms saisis à la création ou modification d'une personne (élève,
 * enseignant, personnel, parent, utilisateur...) — convention administrative courante : prénom en
 * casse de titre par mot (ex. « jean claude » → « Jean Claude », « marie-line » → « Marie-Line »), nom
 * de famille entièrement en majuscules (ex. « camara » → « CAMARA »). Appliqué systématiquement côté
 * serveur pour que la règle tienne quel que soit le point d'entrée (formulaire, import CSV...).
 */
public final class PersonName {

    private static final Pattern WORD = Pattern.compile("[\\p{L}]+");

    private PersonName() {}

    /** « jean claude » → « Jean Claude » ; « marie-line » → « Marie-Line » ; « n'diaye » → « N'Diaye ». */
    public static String capitalizeFirstName(String raw) {
        if (raw == null) return null;
        String trimmed = collapseSpaces(raw);
        if (trimmed.isEmpty()) return trimmed;
        Matcher m = WORD.matcher(trimmed);
        StringBuilder sb = new StringBuilder(trimmed.length());
        int last = 0;
        while (m.find()) {
            sb.append(trimmed, last, m.start());
            String word = m.group();
            sb.append(word.substring(0, 1).toUpperCase(Locale.FRENCH))
                    .append(word.substring(1).toLowerCase(Locale.FRENCH));
            last = m.end();
        }
        sb.append(trimmed.substring(last));
        return sb.toString();
    }

    /** « camara » → « CAMARA ». */
    public static String upperCaseLastName(String raw) {
        if (raw == null) return null;
        return collapseSpaces(raw).toUpperCase(Locale.FRENCH);
    }

    private static String collapseSpaces(String raw) {
        return raw.trim().replaceAll("\\s+", " ");
    }
}
