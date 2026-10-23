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

    /**
     * Variantes de format d'un numéro à tenter ; liste vide si l'identifiant n'a pas l'allure d'un
     * téléphone. Symétrique dans les deux sens : que l'entrée soit déjà au format local
     * ({@code "620999001"}) ou international ({@code "+224620999001"}/{@code "00224620999001"}), les
     * DEUX formes (locale ET internationale) sont générées — sans ça, un numéro saisi en local puis
     * ressaisi en international (ou l'inverse) n'était détecté comme identique que dans UN seul des
     * deux sens, laissant passer un doublon (§retour comptable, incident réel sur l'inscription des
     * parents : {@code ParentService#create}).
     */
    public static List<String> candidates(String id) {
        if (id == null) return List.of();
        String digits = id.replaceAll("[^0-9+]", "");
        if (!digits.matches("\\+?[0-9]{6,}")) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(id.trim());
        out.add(digits);

        String national = digits.startsWith("+") ? digits.substring(1) : digits;
        if (national.startsWith("00")) national = national.substring(2); // 00224... -> 224...

        String local; // sans indicatif pays, ex. "620999001"
        String intl;  // avec indicatif pays, ex. "+224620999001"
        if (national.startsWith("224") && national.length() > 3) {
            local = national.substring(3);
            intl = "+" + national;
        } else {
            local = national;
            intl = "+224" + national;
        }
        out.add(local);
        out.add(intl);
        return List.copyOf(out);
    }
}
