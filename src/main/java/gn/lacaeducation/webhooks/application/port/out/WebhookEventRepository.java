package gn.lacaeducation.webhooks.application.port.out;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface WebhookEventRepository {
    WebhookEvent save(WebhookEvent event);
    Optional<WebhookEvent> findById(Long id);
    List<WebhookEvent> findByEndpointId(Long endpointId);
    List<WebhookEvent> findByStatus(WebhookDeliveryStatus status);
    List<WebhookEvent> findByStatusAndNextAttemptAtBefore(WebhookDeliveryStatus status, LocalDateTime cutoff);
    void deleteById(Long id);
}
