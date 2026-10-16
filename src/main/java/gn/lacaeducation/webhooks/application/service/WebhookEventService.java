package gn.lacaeducation.webhooks.application.service;
import gn.lacaeducation.webhooks.application.port.in.WebhookEventUseCase;
import gn.lacaeducation.webhooks.application.port.out.WebhookEventRepository;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import gn.lacaeducation.common.ResourceNotFoundException;
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
    @Override @Transactional(readOnly=true) public List<WebhookEvent> findByStatus(WebhookDeliveryStatus status) { return repo.findByStatus(status); }
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
        // Même bug que SmsRetryDispatchScheduler (incident de production, DateTimeException "Invalid
        // value for EpochDay") : Math.pow(2, attempts) sature puis déborde au-delà d'environ 64
        // tentatives, sans aucun plafond ici (même pas un Math.min final). Code actuellement mort
        // (aucun appelant dans le reste du code), mais corrigé par précaution avant qu'un futur appel
        // (API manuelle, reprise du mécanisme) ne reproduise le même plantage.
        int cappedAttempts = Math.min(attempts, 10);
        long backoffSeconds = Math.min((long) Math.pow(2, cappedAttempts) * 30, 1800);
        e.setNextAttemptAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        return repo.save(e);
    }
    @Override public WebhookEvent replay(Long id) {
        WebhookEvent e = load(id);
        e.setStatus(WebhookDeliveryStatus.PENDING);
        e.setNextAttemptAt(LocalDateTime.now());
        e.setAttemptCount(0);
        e.setResponseCode(null);
        return repo.save(e);
    }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private WebhookEvent load(Long id) { return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", id)); }
}
