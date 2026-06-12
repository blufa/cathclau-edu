package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.application.port.out.WebhookEventRepository;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import gn.lacaeducation.webhooks.domain.WebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class WebhookEventPersistenceAdapter implements WebhookEventRepository {
    private final WebhookEventJpaRepository jpa;
    private final WebhooksMapper mapper;
    @Override public WebhookEvent save(WebhookEvent e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<WebhookEvent> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<WebhookEvent> findByEndpointId(Long id) { return jpa.findByEndpointId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<WebhookEvent> findByStatus(WebhookDeliveryStatus s) { return jpa.findByStatus(s).stream().map(mapper::toDomain).toList(); }
    @Override public List<WebhookEvent> findByStatusAndNextAttemptAtBefore(WebhookDeliveryStatus s, LocalDateTime t) { return jpa.findByStatusAndNextAttemptAtBefore(s, t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
