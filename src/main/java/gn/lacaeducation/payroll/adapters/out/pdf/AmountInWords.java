package gn.lacaeducation.payroll.adapters.out.pdf;

/**
 * Convertit un montant entier (GNF, jamais de centimes en pratique) en toutes lettres françaises
 * (orthographe traditionnelle, ex. "quatre cent mille", pas la réforme 1990 "quatre-cent-mille") —
 * fidèle à la référence (§document-school, "Fiche de paie.dc.html" : "Quatre cent mille francs
 * guinéens", "Deux millions de francs guinéens"). Le "de" avant "francs" n'apparaît que lorsque le
 * dernier groupe non nul du nombre est un million/milliard (jamais après "mille").
 */
final class AmountInWords {

    private static final String[] UNITS = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
    };
    private static final String[] TENS = {
            null, null, "vingt", "trente", "quarante", "cinquante", "soixante",
    };

    private AmountInWords() {}

    /** @return le montant en toutes lettres, suivi de " francs guinéens" (avec "de" si nécessaire) —
     * {@code null} si {@code value <= 0} (rien de pertinent à écrire en lettres). */
    static String convert(long value) {
        if (value <= 0) return null;
        java.util.List<String> parts = new java.util.ArrayList<>();
        long milliard = value / 1_000_000_000L;
        long million = (value / 1_000_000L) % 1000;
        long thousand = (value / 1000L) % 1000;
        long unit3 = value % 1000;
        boolean lastIsMillionOrMore = false;

        if (milliard > 0) {
            parts.add((milliard == 1 ? "un" : groupOf3(milliard)) + " milliard" + (milliard > 1 ? "s" : ""));
            lastIsMillionOrMore = true;
        }
        if (million > 0) {
            parts.add((million == 1 ? "un" : groupOf3(million)) + " million" + (million > 1 ? "s" : ""));
            lastIsMillionOrMore = true;
        }
        if (thousand > 0) {
            parts.add(thousand == 1 ? "mille" : groupOf3(thousand) + " mille");
            lastIsMillionOrMore = false;
        }
        if (unit3 > 0) {
            parts.add(groupOf3(unit3));
            lastIsMillionOrMore = false;
        }
        if (parts.isEmpty()) return null;

        String words = String.join(" ", parts);
        return words + (lastIsMillionOrMore ? " de francs guinéens" : " francs guinéens");
    }

    /** 0-999. */
    private static String groupOf3(long n) {
        long h = n / 100;
        long r = n % 100;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            if (h == 1) sb.append("cent");
            else sb.append(UNITS[(int) h]).append(" cent").append(r == 0 ? "s" : "");
        }
        if (r > 0) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(groupOf2((int) r));
        }
        return sb.toString();
    }

    /** 1-99. */
    private static String groupOf2(int n) {
        if (n < 17) return UNITS[n];
        if (n < 20) return "dix-" + UNITS[n - 10];
        if (n < 70) {
            int t = n / 10;
            int u = n % 10;
            String tensWord = TENS[t];
            if (u == 0) return tensWord;
            if (u == 1) return tensWord + " et un";
            return tensWord + "-" + UNITS[u];
        }
        if (n < 80) {
            int u = n - 60; // 10..19
            if (u == 11) return "soixante et onze";
            return "soixante-" + tensContinuation(u);
        }
        int u = n - 80; // 0..19
        if (u == 0) return "quatre-vingts";
        return "quatre-vingt-" + tensContinuation(u);
    }

    /** 10-19 → "dix".."dix-neuf", utilisé après "soixante-"/"quatre-vingt-". */
    private static String tensContinuation(int u) {
        if (u < 17) return UNITS[u];
        return "dix-" + UNITS[u - 10];
    }
}
