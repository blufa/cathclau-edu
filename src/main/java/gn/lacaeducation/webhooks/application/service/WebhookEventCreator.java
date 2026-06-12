package gn.lacaeducation.webhooks.application.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gn.lacaeducation.attendance.application.event.StudentAbsentEvent;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import gn.lacaeducation.webhooks.application.port.out.WebhookEndpointRepository;
import gn.lacaeducation.webhooks.application.port.out.WebhookEventRepository;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import gn.lacaeducation.webhooks.domain.WebhookEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookEventCreator {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        createWebhookEvents(WebhookEventType.PAYMENT_RECEIVED, event);
    }

    @EventListener
    public void onReportCardPublished(ReportCardPublishedEvent event) {
        createWebhookEvents(WebhookEventType.REPORT_CARD_AVAILABLE, event);
    }

    @EventListener
    public void onStudentAbsent(StudentAbsentEvent event) {
        createWebhookEvents(WebhookEventType.ABSENCE, event);
    }

    @EventListener
    public void onEnrollmentValidated(EnrollmentValidatedEvent event) {
        createWebhookEvents(WebhookEventType.ENROLLMENT, event);
    }

    private void createWebhookEvents(WebhookEventType eventType, Object event) {
        List<WebhookEndpoint> activeEndpoints = endpointRepository.findAllActive();
        String payload = serializePayload(event);
        for (WebhookEndpoint endpoint : activeEndpoints) {
            if (isSubscribed(endpoint, eventType)) {
                WebhookEvent webhookEvent = WebhookEvent.builder()
                        .endpointId(endpoint.getId())
                        .eventType(eventType)
                        .payload(payload)
                        .status(WebhookDeliveryStatus.PENDING)
                        .attemptCount(0)
                        .nextAttemptAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();
                eventRepository.save(webhookEvent);
                log.debug("Created webhook event {} for endpoint {}", eventType, endpoint.getId());
            }
        }
    }

    private boolean isSubscribed(WebhookEndpoint endpoint, WebhookEventType eventType) {
        String events = endpoint.getEvents();
        if (events == null || events.isBlank()) return false;
        return events.contains(eventType.name());
    }

    private String serializePayload(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook event payload", e);
            return "{}";
        }
    }
}
