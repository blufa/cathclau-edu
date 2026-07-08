package gn.lacaeducation.iam.application.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Génère un identifiant de connexion {@code prenom.nom} (sans accents ni espaces, en minuscules),
 * en ajoutant un suffixe numérique en cas de collision (ex. {@code jean.camara}, {@code jean.camara2}).
 */
public final class UsernameGenerator {

    private UsernameGenerator() {}

    /**
     * @param firstName prénom (peut être nul/vide)
     * @param lastName  nom (peut être nul/vide)
     * @param taken     prédicat indiquant si un username est déjà pris
     * @return un username unique au regard de {@code taken}
     */
    public static String generate(String firstName, String lastName, Predicate<String> taken) {
        String first = slug(firstName);
        String last = slug(lastName);
        String base;
        if (!first.isEmpty() && !last.isEmpty()) base = first + "." + last;
        else if (!first.isEmpty()) base = first;
        else if (!last.isEmpty()) base = last;
        else base = "user";

        String candidate = base;
        int suffix = 2;
        while (taken.test(candidate)) {
            candidate = base + suffix++;
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
