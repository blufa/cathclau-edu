package gn.lacaeducation.communication.adapters.out.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'API Resend (resend.com) — envoi d'e-mails réels aux parents/enseignants.
 * La clé API vient UNIQUEMENT de l'environnement (RESEND_API_KEY), jamais commitée. Tant que
 * {@code enabled=false} ou que la clé/l'expéditeur manquent, le canal EMAIL reste sur SMTP/Mailtrap
 * (voir {@link SmtpEmailGateway}).
 */
@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
        boolean enabled,
        String apiKey,
        /** Expéditeur (ex. « CathClau Edu <no-reply@cathclau-edu.com> »), doit être un domaine vérifié sur Resend. */
        String from,
        String baseUrl) {

    /** Vrai quand l'envoi réel est possible (activé + clé + expéditeur présents). */
    public boolean isConfigured() {
        return enabled && notBlank(apiKey) && notBlank(from);
    }

    public String baseUrlOrDefault() {
        return notBlank(baseUrl) ? baseUrl : "https://api.resend.com";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
