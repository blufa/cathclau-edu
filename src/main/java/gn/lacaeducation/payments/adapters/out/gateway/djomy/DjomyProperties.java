package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Djomy aggregator configuration. Credentials are read from the environment only
 * (DJOMY_CLIENT_ID/SECRET/BASE_URL) and never hard-coded or committed.
 */
@ConfigurationProperties(prefix = "djomy")
public record DjomyProperties(
        String clientId,
        String clientSecret,
        String baseUrl,
        /** ISO currency sent on payment creation (default GNF). */
        String currency,
        /** Frontend return URL the payer is sent back to after the Djomy portal. */
        String returnUrl) {

    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "GNF" : currency;
    }
}
