package gn.lacaeducation.webhooks.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import gn.lacaeducation.payments.DjomyCallbackHandler;
import gn.lacaeducation.webhooks.application.port.out.PaymentWebhookRepository;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Inbound Djomy callback handling (§10.1 + P10):
 *  1. verify the "v1:&lt;hex&gt;" signature over the RAW body BEFORE anything (reject = 400);
 *  2. parse the payload, dedupe on the event id (idempotence), persist the raw record;
 *  3. act only on payment.success (confirmed server-side via verify_payment) and payment.failed.
 * The controller always returns 200 once the signature is valid.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DjomyWebhookService {

    private final DjomyCallbackHandler djomy;
    private final PaymentWebhookRepository repo;
    private final ObjectMapper objectMapper;

    /**
     * @return always true (HTTP 200). The signature is verified best-effort; fulfillment is gated by a
     * server-side {@code verify_payment} call ({@link DjomyCallbackHandler#confirmSuccess}), so a
     * spoofed payload cannot drive a payment to SUCCESS even if the signature is missing/invalid.
     */
    @Transactional
    public boolean handle(String rawBody, String signatureHeader) throws JsonProcessingException {
        // §10.1 : vérification de signature OBLIGATOIRE avant tout traitement. Une signature absente,
        // mal formée ou invalide entraîne un rejet immédiat : aucun enregistrement ni traitement.
        boolean signatureOk = djomy.verifyWebhookSignature(rawBody, signatureHeader);
        if (!signatureOk) {
            String hint = signatureHeader == null || signatureHeader.isBlank()
                    ? "header MISSING (Djomy must send X-Webhook-Signature)"
                    : (signatureHeader.startsWith("v1:")
                        ? "header present but HMAC mismatch (check DJOMY_CLIENT_SECRET matches the dashboard webhook key, and that the raw body is unmodified)"
                        : "header present but wrong format (expected 'v1:<hex>'), got prefix='"
                            + signatureHeader.substring(0, Math.min(6, signatureHeader.length())) + "'");
            log.warn("Djomy webhook signature invalide [{}] — rejeté (aucun traitement).", hint);
            return false;
        }

        String event = null;
        String externalTxId = null;
        String eventId = null;
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            event = text(root, "event", "type", "eventType");
            JsonNode data = root.has("data") ? root.get("data") : root;
            externalTxId = text(data, "transactionId", "transaction_id", "id");
            eventId = text(root, "id", "eventId", "event_id");
        } catch (Exception e) {
            log.warn("Djomy webhook: unparseable body ({})", e.getMessage());
        }
        if (eventId == null || eventId.isBlank()) {
            eventId = nullToEmpty(externalTxId) + ":" + nullToEmpty(event);
        }

        // Idempotence: a previously processed event is acknowledged without re-acting.
        if (repo.existsByExternalEventIdAndProcessedTrue(eventId)) {
            log.info("Djomy webhook already processed: {}", eventId);
            return true;
        }

        Long localTxId = externalTxId == null ? null : djomy.resolveTransactionId(externalTxId).orElse(null);
        PaymentWebhook record = repo.save(PaymentWebhook.builder()
                .transactionId(localTxId)
                .provider(WebhookProvider.DJOMY)
                .event(event)
                .rawPayload(rawBody)
                .signature(signatureHeader)
                .verified(signatureOk)
                .processed(false)
                .externalEventId(eventId)
                .receivedAt(LocalDateTime.now())
                .build());

        boolean acted = false;
        if (externalTxId != null) {
            if ("payment.success".equalsIgnoreCase(event)) {
                // Fulfillment only after server-side verify_payment confirms SUCCESS.
                acted = djomy.confirmSuccess(externalTxId);
            } else if ("payment.failed".equalsIgnoreCase(event)) {
                djomy.markFailed(externalTxId);
                acted = true;
            }
        }
        if (acted) {
            record.setProcessed(true);
            repo.save(record);
        }
        return true;
    }

    private static String text(JsonNode node, String... keys) {
        if (node == null) return null;
        for (String k : keys) {
            if (node.hasNonNull(k)) return node.get(k).asText();
        }
        return null;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
