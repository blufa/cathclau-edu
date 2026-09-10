package gn.lacaeducation.communication.adapters.out.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.application.port.out.SmsBalancePort;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Gateway SMS via l'API NimbaSMS (fournisseur guinéen) — fournisseur SMS actif par défaut pour le
 * canal {@link CommunicationChannel#SMS}.
 *
 * <p>Flux : Basic auth ({@code base64(service_id:secret_token)}) → POST {@code /v1/messages} en JSON
 * {@code {sender_name, to:[numéro...], message}} (confirmé sur developers.nimbasms.com : {@code to}
 * est un tableau de 1 à 30 numéros, {@code sender_name} 1-100 caractères, {@code message} ≤ 1071
 * caractères). NimbaSMS accepte les numéros sous 3 formats ({@code 623XXXXXX}, {@code 224623XXXXXX},
 * {@code +224623XXXXXX}) : pas besoin de forcer l'E.164.
 * Tant que les clés ne sont pas renseignées ({@code nimba.sms.enabled=false}), l'envoi se replie sur
 * la journalisation sans échouer (comportement dev, comme {@link OrangeSmsGateway}).</p>
 *
 * <p>N'est enregistré comme bean que si {@code orange.sms.enabled} n'est PAS explicitement à
 * {@code true} — garantit qu'un seul gateway SMS (Nimba par défaut, Orange sur activation explicite)
 * existe à la fois pour {@link NotificationGateway} et {@link SmsBalancePort}.</p>
 */
@Component
@ConditionalOnProperty(prefix = "orange.sms", name = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NimbaSmsGateway implements NotificationGateway, SmsBalancePort {

    private final NimbaSmsProperties props;
    private final RestClient http;

    public NimbaSmsGateway(NimbaSmsProperties props, RestClient.Builder builder) {
        this.props = props;
        // Le timeout connexion/lecture est appliqué globalement par RestClientTimeoutConfig — sans ça,
        // un appel réseau bloqué (DNS, tunnel, pare-feu) gèlerait indéfiniment le thread de la requête
        // HTTP entrante, le backend « ne répondant plus du tout » à l'utilisateur.
        this.http = builder.baseUrl(props.baseUrlOrDefault())
                .requestInterceptor(new NimbaWireLoggingInterceptor())   // TEMP: remove after diagnostic
                .build();
    }

    @Override
    public CommunicationChannel getChannel() {
        return CommunicationChannel.SMS;
    }

    @Override
    public DeliveryResult send(String destination, String subject, String content) {
        if (destination == null || destination.isBlank()) {
            return new DeliveryResult(false, "no destination");
        }
        String message = (content != null && !content.isBlank()) ? content : subject;
        if (message == null || message.isBlank()) {
            return new DeliveryResult(false, "empty message");
        }
        if (!props.isConfigured()) {
            // Repli tant que Nimba n'est pas configuré : on journalise (comme un stub), sans échouer.
            log.info("[SMS] (Nimba non configuré) -> {} | {}", destination, message);
            return new DeliveryResult(true, "logged (nimba disabled)");
        }
        try {
            MessageRequest body = new MessageRequest(props.senderName(), List.of(normalizeNumber(destination)), message, "sms");
            MessageResponse response = http.post()
                    .uri("/v1/messages")
                    .header("Authorization", "Basic " + basicToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MessageResponse.class);
            boolean ok = response != null && response.messageId() != null && !response.messageId().isBlank();
            log.info("[SMS] Nimba -> {} : {}", destination, ok ? response.messageId() : "réponse invalide");
            return new DeliveryResult(ok, ok ? "nimba " + response.messageId() : "nimba: réponse invalide",
                    ok ? response.messageId() : null);
        } catch (RuntimeException e) {
            log.error("[SMS] échec envoi Nimba vers {} : {}", destination, e.getMessage());
            return new DeliveryResult(false, e.getMessage());
        }
    }

    // ── Solde SMS (GET /v1/accounts) ─────────────────────────────────────────
    @Override
    public List<SmsBalance> fetchBalances() {
        if (!props.isConfigured()) {
            return List.of();
        }
        try {
            AccountResponse account = http.get()
                    .uri("/v1/accounts")
                    .header("Authorization", "Basic " + basicToken())
                    .retrieve()
                    .body(AccountResponse.class);
            if (account == null) return List.of();
            return List.of(new SmsBalance("GN", "NimbaSMS", account.balance(), null, null));
        } catch (RuntimeException e) {
            log.error("[SMS] échec lecture du solde Nimba : {}", e.getMessage());
            return List.of();
        }
    }

    private String basicToken() {
        return Base64.getEncoder().encodeToString(
                (props.serviceId() + ":" + props.secretToken()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Nettoie un numéro pour NimbaSMS : NimbaSMS accepte {@code 623XXXXXX}, {@code 224623XXXXXX} et
     * {@code +224623XXXXXX}, donc pas besoin de forcer un format unique — juste retirer les espaces,
     * tirets et un éventuel préfixe international {@code 00}.
     */
    static String normalizeNumber(String raw) {
        if (raw == null) return null;
        String digits = raw.trim().replaceAll("\\D", "");
        return digits.startsWith("00") ? digits.substring(2) : digits;
    }

    /**
     * Corps de la requête POST /v1/messages (developers.nimbasms.com) : {@code sender_name} [1-100
     * caractères], {@code to} tableau de 1 à 30 numéros [1-17 caractères chacun], {@code message}
     * ≤ 1071 caractères, {@code channel} explicite ("sms" — l'API accepte aussi "whatsapp"/"email",
     * ne pas laisser au défaut implicite). {@code template_name}/{@code template_variables} ne
     * s'appliquent qu'au canal WhatsApp, non utilisés ici.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageRequest(
            @JsonProperty("sender_name") String senderName,
            List<String> to,
            String message,
            String channel) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MessageResponse(
            @JsonProperty("messageid") String messageId,
            @JsonProperty("message_cost") Integer messageCost,
            String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountResponse(String sid, Integer balance) {}
}
