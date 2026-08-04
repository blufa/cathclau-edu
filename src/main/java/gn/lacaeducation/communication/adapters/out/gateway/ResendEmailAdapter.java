package gn.lacaeducation.communication.adapters.out.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Vrai gateway EMAIL via l'API Resend (resend.com) — envoi de courriels réels aux parents et
 * enseignants (rappels d'échéance, bulletins publiés, absences, accueil d'un nouvel établissement…).
 *
 * <p>Remplace {@link SmtpEmailGateway} comme seul bean du canal {@link CommunicationChannel#EMAIL}
 * dès que {@code resend.enabled=true} (voir {@code @ConditionalOnProperty} sur les deux classes —
 * une seule des deux est active à la fois, jamais d'ambiguïté pour {@code CommunicationService}).
 * Tant que Resend n'est pas configuré (clé API manquante), l'envoi se replie sur la journalisation
 * sans échouer, comme {@link gn.lacaeducation.communication.adapters.out.gateway.OrangeSmsGateway}
 * le fait pour le SMS.</p>
 */
@Component
@ConditionalOnProperty(prefix = "resend", name = "enabled", havingValue = "true")
@Slf4j
public class ResendEmailAdapter implements NotificationGateway {

    private final ResendProperties props;
    private final RestClient http;
    private final EmailTemplateRenderer renderer;

    public ResendEmailAdapter(ResendProperties props, RestClient.Builder builder, EmailTemplateRenderer renderer) {
        this.props = props;
        this.http = builder.baseUrl(props.baseUrlOrDefault()).build();
        this.renderer = renderer;
    }

    @Override
    public CommunicationChannel getChannel() {
        return CommunicationChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(String destination, String subject, String content) {
        if (destination == null || destination.isBlank()) {
            return new DeliveryResult(false, "no destination");
        }
        if (!props.isConfigured()) {
            // Repli tant que Resend n'est pas configuré : on journalise (comme un stub), sans échouer.
            log.info("[EMAIL] (Resend non configuré) -> {} | {}", destination, subject);
            return new DeliveryResult(true, "logged (resend disabled)");
        }
        try {
            Map<String, Object> body = Map.of(
                    "from", props.from(),
                    "to", List.of(destination),
                    "subject", subject == null ? "" : subject,
                    "html", renderer.render(subject, content));

            var response = http.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(ResendResponse.class);

            boolean ok = response.getStatusCode().is2xxSuccessful();
            log.info("[EMAIL] Resend -> {} : {} (id={})", destination, response.getStatusCode(),
                    response.getBody() != null ? response.getBody().id() : null);
            return new DeliveryResult(ok, "resend " + response.getStatusCode().value());
        } catch (RuntimeException e) {
            log.error("[EMAIL] échec envoi Resend vers {} : {}", destination, e.getMessage());
            return new DeliveryResult(false, e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResendResponse(String id) {}
}
