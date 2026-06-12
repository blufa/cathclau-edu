package gn.lacaeducation.webhooks.application.service;
import gn.lacaeducation.webhooks.application.port.out.WebhookEndpointRepository;
import gn.lacaeducation.webhooks.application.port.out.WebhookEventRepository;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
public class OutgoingWebhookDispatcher {

    private static final int MAX_ATTEMPTS = 5;
    private static final int BACKOFF_BASE_SECONDS = 30;

    private final WebhookEventRepository eventRepository;
    private final WebhookEndpointRepository endpointRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void dispatchPendingWebhooks() {
        List<WebhookEvent> pending = eventRepository.findByStatusAndNextAttemptAtBefore(
                WebhookDeliveryStatus.PENDING, LocalDateTime.now());
        for (WebhookEvent event : pending) {
            dispatchEvent(event);
        }
    }

    private void dispatchEvent(WebhookEvent event) {
        WebhookEndpoint endpoint = endpointRepository.findById(event.getEndpointId()).orElse(null);
        if (endpoint == null || !Boolean.TRUE.equals(endpoint.getActive())) {
            log.warn("Webhook endpoint {} not found or inactive, skipping event {}", event.getEndpointId(), event.getId());
            return;
        }
        String payload = event.getPayload() != null ? event.getPayload() : "{}";
        String signature = sign(payload, endpoint.getSecret());
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(endpoint.getUrl())
                    .build();
            var response = client.post()
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", "sha256=" + signature)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            int statusCode = response.getStatusCode().value();
            event.setResponseCode(statusCode);
            if (response.getStatusCode().is2xxSuccessful()) {
                event.setStatus(WebhookDeliveryStatus.SENT);
                log.info("Webhook event {} delivered to {} with status {}", event.getId(), endpoint.getUrl(), statusCode);
            } else {
                scheduleRetryOrFail(event, statusCode);
            }
        } catch (RestClientException e) {
            log.warn("Webhook event {} delivery to {} failed: {}", event.getId(), endpoint.getUrl(), e.getMessage());
            scheduleRetryOrFail(event, 0);
        }
        eventRepository.save(event);
    }

    private void scheduleRetryOrFail(WebhookEvent event, int responseCode) {
        int attempts = event.getAttemptCount() + 1;
        event.setAttemptCount(attempts);
        event.setResponseCode(responseCode);
        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(WebhookDeliveryStatus.FAILED);
            log.warn("Webhook event {} permanently failed after {} attempts", event.getId(), attempts);
        } else {
            long backoffSeconds = (long) Math.pow(2, attempts) * BACKOFF_BASE_SECONDS;
            event.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            event.setStatus(WebhookDeliveryStatus.PENDING);
            log.info("Webhook event {} scheduled for retry in {} seconds (attempt {})", event.getId(), backoffSeconds, attempts);
        }
    }

    String sign(String payload, String secret) {
        if (payload == null || secret == null) return "";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to sign webhook payload", e);
            return "";
        }
    }
}
