package gn.lacaeducation.webhooks.application.service;
import gn.lacaeducation.webhooks.application.port.in.WebhookEndpointUseCase;
import gn.lacaeducation.webhooks.application.port.out.WebhookEndpointRepository;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class WebhookEndpointService implements WebhookEndpointUseCase {
    private final WebhookEndpointRepository repo;
    @Override public WebhookEndpoint create(CreateEndpointCommand c) {
        return repo.save(WebhookEndpoint.builder().tenantId(c.tenantId()).url(c.url()).secret(c.secret())
                .events(c.events()).active(true).createdAt(LocalDateTime.now()).build());
    }
    @Override public WebhookEndpoint update(Long id, UpdateEndpointCommand c) {
        WebhookEndpoint ep = load(id); ep.setUrl(c.url()); ep.setSecret(c.secret()); ep.setEvents(c.events()); ep.setActive(c.active());
        return repo.save(ep);
    }
    @Override @Transactional(readOnly=true) public Optional<WebhookEndpoint> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<WebhookEndpoint> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<WebhookEndpoint> findActiveByTenantId(Long t) { return repo.findByTenantIdAndActive(t, true); }
    @Override public void activate(Long id) { WebhookEndpoint ep = load(id); ep.setActive(true); repo.save(ep); }
    @Override public void deactivate(Long id) { WebhookEndpoint ep = load(id); ep.setActive(false); repo.save(ep); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private WebhookEndpoint load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("WebhookEndpoint not found: " + id)); }
}
