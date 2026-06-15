package gn.lacaeducation.webhooks.application.service;
import gn.lacaeducation.payments.PaymentCallbackHandler;
import gn.lacaeducation.webhooks.application.port.in.PaymentWebhookUseCase;
import gn.lacaeducation.webhooks.application.port.out.PaymentWebhookRepository;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor @Slf4j
public class PaymentWebhookService implements PaymentWebhookUseCase {
    private final PaymentWebhookRepository repo;
    private final PaymentCallbackHandler paymentCallbackHandler;
    @Value("${app.webhook.secret:dev-webhook-secret}")
    private String webhookSecret;

    @Override public PaymentWebhook receive(ReceiveWebhookCommand c) {
        // Signature is verified BEFORE any processing (§10.1). The raw payload is always persisted
        // for audit; the payment is only updated when the signature is valid.
        boolean verified = verifyHmacSignature(c.rawPayload(), c.signature(), webhookSecret);
        PaymentWebhook webhook = repo.save(PaymentWebhook.builder().transactionId(c.transactionId()).provider(c.provider())
                .event(c.event()).rawPayload(c.rawPayload()).signature(c.signature())
                .verified(verified).processed(false)
                .externalEventId(c.externalEventId())
                .receivedAt(LocalDateTime.now()).build());

        if (verified && webhook.getTransactionId() != null) {
            boolean success = isSuccessEvent(c.event());
            paymentCallbackHandler.applyCallback(webhook.getTransactionId(), success, c.event(), c.rawPayload());
            webhook.setProcessed(true);
            webhook = repo.save(webhook);
            log.info("Processed payment webhook id={} transaction={} success={}", webhook.getId(), webhook.getTransactionId(), success);
        }
        return webhook;
    }

    /** A callback is a success unless it explicitly signals failure. */
    private boolean isSuccessEvent(String event) {
        if (event == null) return false;
        String e = event.toLowerCase();
        if (e.contains("fail") || e.contains("declin") || e.contains("error") || e.contains("cancel") || e.contains("echou")) {
            return false;
        }
        return e.contains("success") || e.contains("succeed") || e.contains("paid")
                || e.contains("complete") || e.contains("reussi");
    }

    @Override public PaymentWebhook verify(Long id, String secret) {
        PaymentWebhook wh = load(id);
        boolean verified = verifyHmacSignature(wh.getRawPayload(), wh.getSignature(),
                secret != null ? secret : webhookSecret);
        wh.setVerified(verified);
        return repo.save(wh);
    }

    @Override public PaymentWebhook markProcessed(Long id) {
        PaymentWebhook wh = load(id); wh.setProcessed(true); return repo.save(wh);
    }

    @Override @Transactional(readOnly=true) public Optional<PaymentWebhook> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<PaymentWebhook> findUnprocessed() { return repo.findByProcessed(false); }
    @Override @Transactional(readOnly=true) public List<PaymentWebhook> findByProvider(WebhookProvider p) { return repo.findByProvider(p); }

    @Override @Transactional(readOnly=true)
    public boolean isAlreadyProcessed(String externalEventId) {
        // Idempotence keys on a previously *processed* event (§10.1: "traité une seule fois"),
        // so a prior unverified/unprocessed attempt does not block a legitimate retry.
        if (externalEventId == null || externalEventId.isBlank()) return false;
        return repo.existsByExternalEventIdAndProcessedTrue(externalEventId);
    }

    boolean verifyHmacSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.warn("HMAC signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private PaymentWebhook load(Long id) { return repo.findById(id).orElseThrow(() -> new gn.lacaeducation.common.ResourceNotFoundException("PaymentWebhook", id)); }
}
