package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.application.port.out.PaymentWebhookRepository;
import gn.lacaeducation.webhooks.domain.PaymentWebhook;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class PaymentWebhookPersistenceAdapter implements PaymentWebhookRepository {
    private final PaymentWebhookJpaRepository jpa;
    private final WebhooksMapper mapper;
    @Override public PaymentWebhook save(PaymentWebhook wh) { return mapper.toDomain(jpa.save(mapper.toEntity(wh))); }
    @Override public Optional<PaymentWebhook> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<PaymentWebhook> findByProcessed(boolean p) { return jpa.findByProcessed(p).stream().map(mapper::toDomain).toList(); }
    @Override public List<PaymentWebhook> findByProvider(WebhookProvider p) { return jpa.findByProvider(p).stream().map(mapper::toDomain).toList(); }
    @Override public boolean existsByExternalEventId(String externalEventId) { return jpa.existsByExternalEventId(externalEventId); }
}
