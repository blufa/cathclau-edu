package gn.lacaeducation.webhooks.application.service;
import gn.lacaeducation.webhooks.application.port.in.WebhookEventUseCase;
import gn.lacaeducation.webhooks.application.port.out.WebhookEventRepository;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class WebhookEventService implements WebhookEventUseCase {
    private final WebhookEventRepository repo;
    @Override public WebhookEvent create(CreateEventCommand c) {
        return repo.save(WebhookEvent.builder().endpointId(c.endpointId()).eventType(c.eventType())
                .payload(c.payload()).status(WebhookDeliveryStatus.PENDING).attemptCount(0)
                .nextAttemptAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build());
    }
    @Override @Transactional(readOnly=true) public Optional<WebhookEvent> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<WebhookEvent> findByEndpointId(Long id) { return repo.findByEndpointId(id); }
    @Override @Transactional(readOnly=true) public List<WebhookEvent> findPendingEvents() {
        return repo.findByStatusAndNextAttemptAtBefore(WebhookDeliveryStatus.PENDING, LocalDateTime.now());
    }
    @Override public WebhookEvent markSent(Long id, int code) {
        WebhookEvent e = load(id); e.setStatus(WebhookDeliveryStatus.SENT); e.setResponseCode(code); return repo.save(e);
    }
    @Override public WebhookEvent markFailed(Long id, int code) {
        WebhookEvent e = load(id); e.setStatus(WebhookDeliveryStatus.FAILED); e.setResponseCode(code); return repo.save(e);
    }
    @Override public WebhookEvent scheduleRetry(Long id) {
        WebhookEvent e = load(id);
        int attempts = e.getAttemptCount() + 1;
        e.setAttemptCount(attempts);
        e.setStatus(WebhookDeliveryStatus.PENDING);
        // exponential backoff: 2^attempts minutes
        e.setNextAttemptAt(LocalDateTime.now().plusMinutes((long) Math.pow(2, attempts)));
        return repo.save(e);
    }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private WebhookEvent load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("WebhookEvent not found: " + id)); }
}
