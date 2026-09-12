package gn.lacaeducation.billing.domain;

/** Libellés français d'un mois "YYYY-MM" — évite de dupliquer le tableau des noms de mois partout où
 * une échéance ("Installment.months") doit s'afficher lisiblement (facture éclatée mois par mois, PDF). */
public final class MonthLabel {

    private static final String[] MOIS_FR = {"janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"};

    private MonthLabel() {}

    /** "2026-10" → "octobre" (nom seul, sans l'année). */
    public static String shortLabel(String yyyyMm) {
        int month = monthOf(yyyyMm);
        return month == -1 ? yyyyMm : MOIS_FR[month - 1];
    }

    /** "2026-10" → "Octobre 2026" (libellé complet, pour un objet de facture par ex.). */
    public static String frenchLabel(String yyyyMm) {
        int month = monthOf(yyyyMm);
        if (month == -1) return yyyyMm;
        String name = MOIS_FR[month - 1];
        String year = yyyyMm.substring(0, 4);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + year;
    }

    private static int monthOf(String yyyyMm) {
        if (yyyyMm == null || yyyyMm.length() < 7) return -1;
        try {
            int month = Integer.parseInt(yyyyMm.substring(5, 7));
            return month >= 1 && month <= 12 ? month : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
