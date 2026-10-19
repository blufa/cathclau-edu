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
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vrai gateway SMS via l'API Orange (Orange Developer) — remplace le stub de journalisation pour le
 * canal {@link CommunicationChannel#SMS}.
 *
 * <p>Flux : OAuth2 client_credentials (jeton caché jusqu'à expiration) → POST
 * {@code /smsmessaging/v1/outbound/{senderAddress}/requests}. Le numéro est normalisé en E.164
 * ({@code tel:+224…}). Tant que les clés Orange ne sont pas renseignées ({@code orange.sms.enabled=false}),
 * l'envoi se replie sur la journalisation sans échouer (comportement dev, comme l'ancien stub).</p>
 *
 * <p>N'est enregistré comme bean QUE si {@code orange.sms.enabled=true} : {@link NimbaSmsGateway} est
 * le fournisseur SMS actif par défaut, et un seul bean {@link NotificationGateway}/{@link SmsBalancePort}
 * pour le canal SMS doit exister à la fois (sinon la sélection dans {@code CommunicationService}
 * deviendrait non déterministe).</p>
 */
@Component
@ConditionalOnProperty(prefix = "orange.sms", name = "enabled", havingValue = "true")
@Slf4j
public class OrangeSmsGateway implements NotificationGateway, SmsBalancePort {

    private final OrangeSmsProperties props;
    private final RestClient http;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    public OrangeSmsGateway(OrangeSmsProperties props, RestClient.Builder builder) {
        this.props = props;
        // Timeout connexion/lecture appliqué globalement par RestClientTimeoutConfig.
        this.http = builder.baseUrl(props.baseUrlOrDefault()).build();
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
            // Même correctif que NimbaSmsGateway (§retour fondateur) : delivered=true ici rendait un SMS
            // jamais réellement envoyé indiscernable d'une vraie livraison. delivered=false +
            // retryable=false : échec définitif visible, pas de blocage du flux appelant (paiement…), pas
            // de retenter indéfiniment une config manquante.
            log.warn("[SMS] Orange NON CONFIGURÉ — SMS NON envoyé (pas d'appel réseau) vers {} : {}", destination, message);
            return new DeliveryResult(false, "orange non configuré (clés manquantes) : SMS non envoyé", null, false);
        }
        try {
            String to = toE164(destination, props.defaultDialingCode());
            String sender = toE164(props.senderAddress(), props.defaultDialingCode());

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("address", "tel:" + to);
            request.put("senderAddress", "tel:" + sender);
            if (props.senderName() != null && !props.senderName().isBlank()) {
                request.put("senderName", props.senderName());
            }
            request.put("outboundSMSTextMessage", Map.of("message", message));
            Map<String, Object> body = Map.of("outboundSMSMessageRequest", request);

            // {sender} est encodé par UriBuilder (tel:+224… → tel%3A%2B224…), comme l'exige Orange.
            var response = http.post()
                    .uri(b -> b.path("/smsmessaging/v1/outbound/{sender}/requests").build("tel:" + sender))
                    .header("Authorization", "Bearer " + bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            boolean ok = response.getStatusCode().is2xxSuccessful();
            log.info("[SMS] Orange -> {} : {}", to, response.getStatusCode());
            return new DeliveryResult(ok, "orange " + response.getStatusCode().value());
        } catch (RuntimeException e) {
            log.error("[SMS] échec envoi Orange vers {} : {}", destination, e.getMessage());
            return new DeliveryResult(false, e.getMessage());
        }
    }

    // ── Solde SMS (GET /sms/admin/v1/contracts) ──────────────────────────────
    @Override
    public List<SmsBalance> fetchBalances() {
        if (!props.isConfigured()) {
            return List.of();
        }
        try {
            OrangeContract[] contracts = http.get()
                    .uri("/sms/admin/v1/contracts")
                    .header("Authorization", "Bearer " + bearer())
                    .retrieve()
                    .body(OrangeContract[].class);
            if (contracts == null) return List.of();
            return java.util.Arrays.stream(contracts)
                    .map(c -> new SmsBalance(c.country(), c.offerName(), c.availableUnits(),
                            c.status(), c.expirationDate()))
                    .toList();
        } catch (RuntimeException e) {
            log.error("[SMS] échec lecture du solde Orange : {}", e.getMessage());
            return List.of();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OrangeContract(String country, String offerName, Integer availableUnits,
                          String status, String expirationDate) {}

    // ── OAuth2 (jeton caché jusqu'à expiration) ──────────────────────────────
    private synchronized String bearer() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        String basic = Base64.getEncoder().encodeToString(
                (props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8));
        TokenResponse resp = http.post()
                .uri("/oauth/v3/token")
                .header("Authorization", "Basic " + basic)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(TokenResponse.class);
        if (resp == null || resp.accessToken() == null || resp.accessToken().isBlank()) {
            throw new IllegalStateException("Orange OAuth2 n'a renvoyé aucun jeton "
                    + "(vérifier ORANGE_SMS_CLIENT_ID / ORANGE_SMS_CLIENT_SECRET)");
        }
        cachedToken = resp.accessToken();
        long ttl = resp.ttlSeconds();
        tokenExpiry = Instant.now().plusSeconds(Math.max(60L, ttl - 60L));
        return cachedToken;
    }

    /**
     * Normalise un numéro en E.164 avec {@code +} (ex. {@code +224620000000}). Gère les formats locaux
     * ({@code 620…}), {@code 00224…}, {@code 224…} et {@code +224…}.
     */
    static String toE164(String raw, String defaultDialingCode) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        boolean hadPlus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D", "");
        if (hadPlus) return "+" + digits;
        if (digits.startsWith("00")) return "+" + digits.substring(2);
        if (digits.startsWith(defaultDialingCode)) return "+" + digits;
        return "+" + defaultDialingCode + digits;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") String expiresIn) {
        long ttlSeconds() {
            try {
                return expiresIn == null || expiresIn.isBlank() ? 3600L : Long.parseLong(expiresIn.trim());
            } catch (NumberFormatException e) {
                return 3600L;
            }
        }
    }
}
