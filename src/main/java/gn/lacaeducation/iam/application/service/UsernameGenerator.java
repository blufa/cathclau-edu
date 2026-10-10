package gn.lacaeducation.iam.application.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Génère un identifiant de connexion {@code nom@numero} — nom de famille (sans accents ni espaces, en
 * minuscules) suivi d'un numéro à 3 chiffres (ex. {@code soumah@345}). En cas de collision, le numéro
 * est incrémenté ({@code soumah@346}, etc.) plutôt que retiré au hasard, pour converger vite même avec
 * beaucoup d'homonymes.
 */
public final class UsernameGenerator {

    private UsernameGenerator() {}

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * @param firstName prénom (repli si le nom est vide ; peut être nul/vide)
     * @param lastName  nom de famille (peut être nul/vide)
     * @param taken     prédicat indiquant si un username est déjà pris
     * @return un username unique au regard de {@code taken}
     */
    public static String generate(String firstName, String lastName, Predicate<String> taken) {
        String last = slug(lastName);
        String first = slug(firstName);
        String base;
        if (!last.isEmpty()) base = last;
        else if (!first.isEmpty()) base = first;
        else base = "user";

        int number = 100 + RANDOM.nextInt(900); // 100–999, comme l'exemple "soumah@345"
        String candidate = base + "@" + number;
        while (taken.test(candidate)) {
            candidate = base + "@" + (++number);
        }
        return candidate;
    }

    /** Minuscules, sans accents, uniquement [a-z0-9]. */
    private static String slug(String value) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
