package gn.lacaeducation.common;

import java.time.Year;
import java.util.Locale;

/**
 * Génère un matricule (élève ou enseignant) selon un format librement défini par l'école, via des
 * jetons remplacés tels quels : {@code {PREFIXE}} (sigle de l'école, voir {@link SchoolCode}),
 * {@code {ANNEE}} (année civile en cours), {@code {NUM}} (numéro séquentiel, complété de zéros de
 * tête). Un format absent équivaut au comportement historique fixe {@code {PREFIXE}{NUM}} (ex.
 * "AT00001") — aucune migration de données requise pour les écoles existantes.
 */
public final class MatriculeFormatter {

    private static final String DEFAULT_FORMAT = "{PREFIXE}{NUM}";
    private static final int DEFAULT_DIGITS = 5;
    private static final int MAX_DIGITS = 10;

    private MatriculeFormatter() {
    }

    public static String generate(String format, Integer digits, String prefix, long seq) {
        String fmt = (format == null || format.isBlank()) ? DEFAULT_FORMAT : format;
        int width = (digits == null || digits < 1) ? DEFAULT_DIGITS : Math.min(digits, MAX_DIGITS);
        String num = String.format(Locale.ROOT, "%0" + width + "d", seq);
        String year = String.valueOf(Year.now().getValue());
        return fmt.replace("{PREFIXE}", prefix == null ? "" : prefix)
                .replace("{ANNEE}", year)
                .replace("{NUM}", num);
    }
}
