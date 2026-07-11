package gn.lacaeducation.communication.adapters.out.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'API SMS Orange (Orange Developer). Les identifiants viennent UNIQUEMENT de
 * l'environnement (ORANGE_SMS_*), jamais commités. Tant que {@code enabled=false} ou que les clés
 * manquent, l'adaptateur se replie sur la journalisation (voir {@link OrangeSmsGateway}).
 */
@ConfigurationProperties(prefix = "orange.sms")
public record OrangeSmsProperties(
        boolean enabled,
        String clientId,
        String clientSecret,
        /** Base de l'API Orange (défaut https://api.orange.com). */
        String baseUrl,
        /** Numéro/expéditeur Orange autorisé (ex. +224XXXXXXXX). */
        String senderAddress,
        /** Sender ID alphanumérique optionnel (ex. « CathClau »), s'il est approuvé par Orange. */
        String senderName,
        /** Indicatif pays par défaut pour normaliser les numéros locaux (Guinée = 224). */
        String defaultCountryCode) {

    /** Vrai quand l'envoi réel est possible (activé + clés + expéditeur présents). */
    public boolean isConfigured() {
        return enabled && notBlank(clientId) && notBlank(clientSecret) && notBlank(senderAddress);
    }

    public String baseUrlOrDefault() {
        return notBlank(baseUrl) ? baseUrl : "https://api.orange.com";
    }

    public String defaultDialingCode() {
        String d = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("\\D", "");
        return d.isBlank() ? "224" : d;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
