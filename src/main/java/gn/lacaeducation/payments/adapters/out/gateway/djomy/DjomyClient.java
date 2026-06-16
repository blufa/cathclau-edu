package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * NOTE: endpoint paths and response field names below follow the spec where given and use
 * resilient @JsonAlias for the rest — confirm against the live Djomy API reference.
 */
@Component
public class DjomyClient {

    private final DjomyProperties props;
    private final RestClient http;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    public DjomyClient(DjomyProperties props, RestClient.Builder builder) {
        this.props = props;
        this.http = builder.baseUrl(props.baseUrl() == null ? "" : props.baseUrl()).build();
    }

    // ── public API ──────────────────────────────────────────────────────────
    public record GatewayResult(String transactionId, String redirectUrl) {}

    public record Verification(String status, BigDecimal receivedAmount, String currency) {
        public boolean isSuccess() {
            return "SUCCESS".equalsIgnoreCase(status);
        }
    }

    /** Default flow: delegates payment method + OTP to the Djomy portal; payerNumber is required. */
    public GatewayResult createPaymentGateway(String payerNumber, BigDecimal amount, String reference, String description) {
        if (payerNumber == null || payerNumber.isBlank()) {
            throw new IllegalArgumentException("Djomy requires a payer phone number (payerNumber)");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentMethod", "create_payment_gateway");
        body.put("payerNumber", payerNumber);
        body.put("amount", amount);
        body.put("currency", props.currencyOrDefault());
        body.put("reference", reference);
        body.put("description", description == null ? reference : description);
        body.put("returnUrl", props.returnUrl() == null ? "" : props.returnUrl());

        CreateResponse resp = http.post()
                .uri("/v1/payments")
                .header("Authorization", "Bearer " + bearer())
                .header("X-API-KEY", DjomySignature.apiKey(props.clientId(), props.clientSecret()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(CreateResponse.class);
        return new GatewayResult(resp.transactionId(), resp.redirectUrl());
    }

    /** Server-side confirmation: must be called before validating anything (status === 'SUCCESS'). */
    public Verification verifyPayment(String transactionId) {
        VerifyResponse resp = http.get()
                .uri("/v1/payments/{id}", transactionId)
                .header("Authorization", "Bearer " + bearer())
                .header("X-API-KEY", DjomySignature.apiKey(props.clientId(), props.clientSecret()))
                .retrieve()
                .body(VerifyResponse.class);
        // Use receivedAmount (net after fees) for accounting, not paidAmount.
        BigDecimal net = resp.receivedAmount() != null ? resp.receivedAmount() : resp.paidAmount();
        return new Verification(resp.status(), net, DjomyCurrency.toIso(resp.currency()));
    }

    // ── auth (token cached until expiry) ─────────────────────────────────────
    private synchronized String bearer() {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        AuthResponse resp = http.post()
                .uri("/v1/auth")
                .header("X-API-KEY", DjomySignature.apiKey(props.clientId(), props.clientSecret()))
                .retrieve()
                .body(AuthResponse.class);
        cachedToken = resp.token();
        long ttl = resp.expiresIn() != null ? resp.expiresIn() : 3600L;
        tokenExpiry = Instant.now().plusSeconds(Math.max(60L, ttl - 60L));
        return cachedToken;
    }

    // ── response DTOs (resilient to field naming) ────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthResponse(
            @JsonAlias({"token", "accessToken", "access_token"}) String token,
            @JsonAlias({"expiresIn", "expires_in", "expiry"}) Long expiresIn) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CreateResponse(
            @JsonAlias({"transactionId", "transaction_id", "id"}) String transactionId,
            @JsonAlias({"redirectUrl", "redirect_url", "paymentUrl"}) String redirectUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record VerifyResponse(
            @JsonAlias({"status", "paymentStatus"}) String status,
            @JsonAlias({"receivedAmount", "received_amount"}) BigDecimal receivedAmount,
            @JsonAlias({"paidAmount", "paid_amount"}) BigDecimal paidAmount,
            @JsonAlias({"currency"}) String currency) {}
}
