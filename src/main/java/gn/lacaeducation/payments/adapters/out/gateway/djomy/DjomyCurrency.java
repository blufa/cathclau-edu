package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import java.util.Map;

/**
 * Maps the non-ISO currency labels Djomy may return ('GNF','FCFA','Dollar','Leones')
 * to ISO 4217 codes (GNF, XOF, USD, SLE). Unknown/null values are returned unchanged.
 */
public final class DjomyCurrency {

    private DjomyCurrency() {}

    private static final Map<String, String> TO_ISO = Map.of(
            "GNF", "GNF",
            "FCFA", "XOF",
            "DOLLAR", "USD",
            "LEONES", "SLE");

    public static String toIso(String currency) {
        if (currency == null) return null;
        return TO_ISO.getOrDefault(currency.trim().toUpperCase(), currency);
    }
}
