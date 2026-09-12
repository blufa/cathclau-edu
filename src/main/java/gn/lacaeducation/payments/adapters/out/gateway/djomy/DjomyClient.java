package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP client for the Djomy aggregator (sandbox/prod base URL from config).
 *
 * Auth (P9): HMAC-SHA256(clientId, clientSecret) hex → POST /v1/auth with header
 * "X-API-KEY: {clientId}:{hex}" → Bearer token (cached until expiry). Every authenticated
 * request carries BOTH "Authorization: Bearer {token}" AND the recomputed "X-API-KEY".
 *
 * Payment creation uses the redirect flow {@code POST /v1/payments/gateway}: it returns a
 * portal redirectUrl for every method (mobile money AND cards), unlike the direct
 * {@code POST /v1/payments} which has no redirect for OM/MOMO and forbids cards.
 */
@Component
@Slf4j
public class DjomyClient {

    private final DjomyProperties props;
    private final RestClient http;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    public DjomyClient(DjomyProperties props, RestClient.Builder builder) {
        this.props = props;
        // Timeout connexion/lecture appliqué globalement par RestClientTimeoutConfig.
        this.http = builder.baseUrl(props.baseUrl() == null ? "" : props.baseUrl())
                .requestInterceptor(new DjomyWireLoggingInterceptor())   // TEMP: remove after testing
                .build();
    }

    // ── public API ──────────────────────────────────────────────────────────
    public record GatewayResult(String transactionId, String redirectUrl) {}

    public record Verification(String status, BigDecimal receivedAmount, String currency) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    /**
     * Initiates a Djomy payment via the redirect portal ({@code POST /v1/payments/gateway}). The
     * portal lets the payer pick any configured method (djomy.allowed-methods) and always returns a
     * redirect URL. {@code payerNumber} may be a local number — it is normalised to the
     * international format Djomy requires (e.g. 00224…); country comes from djomy.default-country.
     */
    public GatewayResult createPaymentGateway(String payerNumber, BigDecimal amount, String reference, String description) throws JsonProcessingException {
        if (props.clientId() == null || props.clientId().isBlank()
                || props.clientSecret() == null || props.clientSecret().isBlank()) {
            throw new IllegalStateException("Djomy credentials are not configured "
                    + "(set DJOMY_CLIENT_ID / DJOMY_CLIENT_SECRET) — use a non-Djomy channel for local testing");
        }
        if (payerNumber == null || payerNumber.isBlank()) {
            throw new IllegalArgumentException("Djomy requires a payer phone number (payerNumber)");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("countryCode", props.countryOrDefault());
        body.put("payerNumber", toInternational(payerNumber, props.countryOrDefault()));
        body.put("allowedPaymentMethods", props.allowedMethodsOrDefault());
        body.put("description", description == null ? reference : description);
        body.put("merchantPaymentReference", reference);
        body.put("returnUrl", props.returnUrl() == null ? "" : props.returnUrl());
        body.put("cancelUrl", props.cancelUrlOrReturn() == null ? "" : props.cancelUrlOrReturn());

        log.info("DJOMY CREATE PAYMENT ref={} amount={} country={} methods={} normalized={}",
                reference, amount, props.countryOrDefault(), props.allowedMethodsOrDefault(),
                toInternational(payerNumber, props.countryOrDefault()));

        String rawResponse = http.post()
                .uri("/v1/payments/gateway")
                .header("Authorization", "Bearer " + bearer())
                .header("X-API-KEY", DjomySignature.apiKey(props.clientId(), props.clientSecret()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        log.info("DJOMY RAW RESPONSE : {}", rawResponse);

        CreateResponse resp = new ObjectMapper().readValue(rawResponse, CreateResponse.class);
        log.info("DJOMY transactionId={} status={}", resp.transactionId(), resp.status());
        return new GatewayResult(resp.transactionId(), resp.redirectUrl());
    }

    /** ISO country code → international dialing code (West-African scope; extend as needed). */
    private static final Map<String, String> DIALING_CODES = Map.of(
            "GN", "224", "CI", "225", "SN", "221", "ML", "223",
            "BF", "226", "TG", "228", "BJ", "229", "NE", "227");

    /**
     * Normalises a phone number to Djomy's international format (e.g. {@code 00224623707722}).
     * Accepts already-international inputs ({@code 00…} or {@code +…}) and local numbers, which are
     * prefixed with {@code 00 + dialing code} for the configured country.
     */
    static String toInternational(String raw, String countryCode) {
        String n = raw.replaceAll("[\\s\\-()]", "");
        if (n.startsWith("+")) return "00" + n.substring(1);
        if (n.startsWith("00")) return n;
        if (n.startsWith("0")) n = n.substring(1);
        String dialing = DIALING_CODES.getOrDefault(
                countryCode == null ? "" : countryCode.toUpperCase(), "224");
        return "00" + dialing + n;
    }

    /** Server-side confirmation: must be called before validating anything (status === 'SUCCESS'). */
    public Verification verifyPayment(String transactionId) throws JsonProcessingException {

        String rawVerifyResponse = http.get()
                .uri("/v1/payments/{id}/status", transactionId)
                .header("Authorization", "Bearer " + bearer())
                .header("X-API-KEY",
                        DjomySignature.apiKey(
                                props.clientId(),
                                props.clientSecret()))
                .retrieve()
                .body(String.class);

        log.info("DJOMY VERIFY RESPONSE : {}", rawVerifyResponse);

        VerifyResponse resp = new ObjectMapper().readValue(rawVerifyResponse, VerifyResponse.class);

        // Use receivedAmount (net after fees) for accounting, not paidAmount.
        BigDecimal net = resp.receivedAmount() != null ? resp.receivedAmount() : resp.paidAmount();
        return new Verification(resp.status(), net, DjomyCurrency.toIso(resp.currency()));
    }

    // ── auth (token cached until expiry) ─────────────────────────────────────
    private synchronized String bearer() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        log.info("Calling DJOMY AUTH...");

        AuthResponse resp = http.post()
                .uri("/v1/auth")
                .header("X-API-KEY", DjomySignature.apiKey(props.clientId(), props.clientSecret()))
                .retrieve()
                .body(AuthResponse.class);
        String token = resp != null ? resp.token() : null;

        log.info("DJOMY AUTH ok (expiresIn={})", resp != null ? resp.expiresIn() : null);

        if (token == null || token.isBlank()) {
            // A null token here means Djomy authenticated but our DTO did not capture the token
            // field (different name/nesting), or the credentials are invalid. Fail clearly instead
            // of sending "Bearer null" downstream (which Djomy rejects as "Malformed token").
            throw new IllegalStateException("Djomy authentication returned no access token "
                    + "(check DJOMY_CLIENT_ID / DJOMY_CLIENT_SECRET and the /v1/auth response shape)");
        }
        cachedToken = token;
        long ttl = resp.expiresIn() != null ? resp.expiresIn() : 3600L;
        tokenExpiry = Instant.now().plusSeconds(Math.max(60L, ttl - 60L));
        return cachedToken;
    }

    // ── response DTOs ────────────────────────────────────────────────────────
    // Every Djomy response wraps the payload in a "data" envelope:
    //   { "success": true, "data": { ... }, "status": 200 }
    // The accessor methods flatten it so the calling code stays unchanged.

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthResponse(Data data) {
        String token() { return data != null ? data.accessToken() : null; }
        Long expiresIn() { return data != null ? data.expiresIn() : null; }
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Data(String accessToken, Long expiresIn) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreateResponse(Data data) {
        String transactionId() { return data != null ? data.transactionId() : null; }
        String status() { return data != null ? data.status() : null; }
        /** Portal redirect URL; falls back to paymentUrl when only the latter is returned. */
        String redirectUrl() {
            if (data == null) return null;
            return data.redirectUrl() != null ? data.redirectUrl() : data.paymentUrl();
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Data(
                @JsonAlias({"transaction_id", "id"}) String transactionId,
                String status,
                @JsonAlias({"redirect_url"}) String redirectUrl,
                @JsonAlias({"payment_url"}) String paymentUrl) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VerifyResponse(Data data) {
        String status() { return data != null ? data.status() : null; }
        BigDecimal receivedAmount() { return data != null ? data.receivedAmount() : null; }
        BigDecimal paidAmount() { return data != null ? data.paidAmount() : null; }
        String currency() { return data != null ? data.currency() : null; }
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Data(
                @JsonAlias({"paymentStatus"}) String status,
                @JsonAlias({"received_amount"}) BigDecimal receivedAmount,
                @JsonAlias({"paid_amount"}) BigDecimal paidAmount,
                String currency) {}
    }
}
