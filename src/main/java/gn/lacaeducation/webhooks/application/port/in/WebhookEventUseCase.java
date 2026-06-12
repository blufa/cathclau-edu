package gn.lacaeducation.webhooks.application.port.in;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import gn.lacaeducation.webhooks.domain.WebhookEventType;
import java.util.List;
import java.util.Optional;
public interface WebhookEventUseCase {
    record CreateEventCommand(Long endpointId, WebhookEventType eventType, String payload) {}
    WebhookEvent create(CreateEventCommand command);
    Optional<WebhookEvent> findById(Long id);
    List<WebhookEvent> findByEndpointId(Long endpointId);
    List<WebhookEvent> findByStatus(WebhookDeliveryStatus status);
    List<WebhookEvent> findPendingEvents();
    WebhookEvent markSent(Long id, int responseCode);
    WebhookEvent markFailed(Long id, int responseCode);
    WebhookEvent scheduleRetry(Long id);
    WebhookEvent replay(Long id);
    void delete(Long id);
}
