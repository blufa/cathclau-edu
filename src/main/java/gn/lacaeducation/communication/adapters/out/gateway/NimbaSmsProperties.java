package gn.lacaeducation.communication.adapters.out.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'API SMS NimbaSMS. Les identifiants viennent UNIQUEMENT de l'environnement
 * (NIMBA_SMS_*), jamais commités. Tant que {@code enabled=false} ou que les clés manquent,
 * l'adaptateur se replie sur la journalisation (voir {@link NimbaSmsGateway}).
 */
@ConfigurationProperties(prefix = "nimba.sms")
public record NimbaSmsProperties(
        boolean enabled,
        /** service_id (API KEYS NimbaSMS). */
        String serviceId,
        /** secret_token (API KEYS NimbaSMS). */
        String secretToken,
        /** Base de l'API NimbaSMS (défaut https://api.nimbasms.com). */
        String baseUrl,
        /** Nom d'expéditeur (sender_name), sensible à la casse. */
        String senderName,
        /** Indicatif pays par défaut pour les numéros locaux (Guinée = 224). */
        String defaultCountryCode) {

    /** Vrai quand l'envoi réel est possible (activé + clés + sender_name présents). */
    public boolean isConfigured() {
        return enabled && notBlank(serviceId) && notBlank(secretToken) && notBlank(senderName);
    }

    public String baseUrlOrDefault() {
        return notBlank(baseUrl) ? baseUrl : "https://api.nimbasms.com";
    }

    public String defaultDialingCode() {
        String d = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("\\D", "");
        return d.isBlank() ? "224" : d;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
