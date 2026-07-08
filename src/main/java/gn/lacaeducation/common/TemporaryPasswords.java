package gn.lacaeducation.common;

import java.security.SecureRandom;

/**
 * Génère un mot de passe temporaire <b>lisible</b> pour un nouveau compte (ex. compte admin créé par
 * le SUPER_ADMIN sans mot de passe fourni). Format {@code Abcd-1234-Efgh} : trois groupes séparés par
 * des tirets, sans caractères ambigus (0/O, 1/l/I), avec au moins une majuscule, une minuscule et un
 * chiffre. À usage unique : l'utilisateur doit le changer à la première connexion.
 */
public final class TemporaryPasswords {

    private TemporaryPasswords() {}

    private static final char[] UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();   // sans I, O
    private static final char[] LOWER = "abcdefghijkmnpqrstuvwxyz".toCharArray();   // sans l, o
    private static final char[] DIGITS = "23456789".toCharArray();                 // sans 0, 1
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Ex. {@code Kdrp-4827-Muxs} (14 caractères). */
    public static String generate() {
        return letters(UPPER, LOWER, 4) + "-" + digits(4) + "-" + letters(UPPER, LOWER, 4);
    }

    private static String letters(char[] upper, char[] lower, int count) {
        StringBuilder sb = new StringBuilder(count);
        // Première lettre en majuscule, le reste en minuscules (lisible).
        sb.append(upper[RANDOM.nextInt(upper.length)]);
        for (int i = 1; i < count; i++) {
            sb.append(lower[RANDOM.nextInt(lower.length)]);
        }
        return sb.toString();
    }

    private static String digits(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(DIGITS[RANDOM.nextInt(DIGITS.length)]);
        }
        return sb.toString();
    }
}
