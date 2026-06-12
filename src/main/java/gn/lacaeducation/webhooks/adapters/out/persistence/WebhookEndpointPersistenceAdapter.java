package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.application.port.out.WebhookEndpointRepository;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class WebhookEndpointPersistenceAdapter implements WebhookEndpointRepository {
    private final WebhookEndpointJpaRepository jpa;
    private final WebhooksMapper mapper;
    @Override public WebhookEndpoint save(WebhookEndpoint ep) { return mapper.toDomain(jpa.save(mapper.toEntity(ep))); }
    @Override public Optional<WebhookEndpoint> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<WebhookEndpoint> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<WebhookEndpoint> findByTenantIdAndActive(Long t, boolean a) { return jpa.findByTenantIdAndActive(t, a).stream().map(mapper::toDomain).toList(); }
    @Override public List<WebhookEndpoint> findAllActive() { return jpa.findByActive(true).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpa.existsById(id); }
}
