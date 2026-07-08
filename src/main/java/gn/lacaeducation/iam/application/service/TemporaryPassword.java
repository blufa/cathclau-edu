package gn.lacaeducation.iam.application.service;

import java.security.SecureRandom;

/**
 * Génère un mot de passe temporaire lisible (sans caractères ambigus) à communiquer à l'utilisateur,
 * qu'il devra changer à la première connexion.
 */
final class TemporaryPassword {

    // Alphabet sans caractères ambigus (pas de O/0, I/l/1) pour faciliter la saisie/dictée.
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789".toCharArray();
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPassword() {}

    static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
