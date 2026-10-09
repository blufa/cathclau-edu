package gn.lacaeducation.common;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Un même numéro peut être saisi sous plusieurs formats (local, international +224, avec ou sans
 * espaces) selon qui l'a entré et quand. Centralise la génération des variantes à essayer — utilisé
 * à la fois pour résoudre un identifiant de connexion ({@code AuthService}) et pour détecter un
 * doublon avant de l'enregistrer ({@code UserService}) : les deux doivent reconnaître les mêmes
 * numéros comme identiques, sinon un numéro jugé "libre" à l'enregistrement peut ensuite résoudre
 * vers un autre compte à la connexion.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {}

    /** Variantes de format d'un numéro à tenter ; liste vide si l'identifiant n'a pas l'allure d'un téléphone. */
    public static List<String> candidates(String id) {
        if (id == null) return List.of();
        String digits = id.replaceAll("[^0-9+]", "");
        if (!digits.matches("\\+?[0-9]{6,}")) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(id.trim());
        out.add(digits);
        String d = digits.startsWith("+") ? digits.substring(1) : digits;
        if (digits.startsWith("+")) out.add(digits);
        else if (d.startsWith("00")) out.add("+" + d.substring(2));
        else if (d.startsWith("224")) out.add("+" + d);
        else out.add("+224" + d); // numéro local guinéen
        return List.copyOf(out);
    }
}
