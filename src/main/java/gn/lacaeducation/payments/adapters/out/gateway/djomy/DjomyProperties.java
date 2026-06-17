package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Djomy aggregator configuration. Credentials are read from the environment only
 * (DJOMY_CLIENT_ID/SECRET/BASE_URL) and never hard-coded or committed.
 */
@ConfigurationProperties(prefix = "djomy")
public record DjomyProperties(
        String clientId,
        String clientSecret,
        String baseUrl,
        /** ISO currency (informational; Djomy derives the currency from the country). */
        String currency,
        /** Frontend return URL the payer is sent back to after a successful/failed payment. */
        String returnUrl,
        /** Frontend URL used when the payer abandons the portal; falls back to returnUrl. */
        String cancelUrl,
        /** ISO country code sent on payment creation (default GN). */
        String defaultCountry,
        /** Methods shown on the Djomy portal (allowedPaymentMethods); default = all portal methods. */
        List<String> allowedMethods) {

    public String currencyOrDefault() {
        return currency == null || currency.isBlank() ? "GNF" : currency;
    }

    public String countryOrDefault() {
        return defaultCountry == null || defaultCountry.isBlank() ? "GN" : defaultCountry;
    }

    /** Cancel URL or, when unset, the return URL (Djomy requires HTTPS for both in production). */
    public String cancelUrlOrReturn() {
        return cancelUrl == null || cancelUrl.isBlank() ? returnUrl : cancelUrl;
    }

    /** Configured portal methods, or every portal-capable method when left unset. */
    public List<String> allowedMethodsOrDefault() {
        return allowedMethods == null || allowedMethods.isEmpty()
                ? DjomyPaymentMethod.portalMethods()
                : allowedMethods;
    }
}
