package gn.lacaeducation.webhooks.application.port.out;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import java.util.List;
import java.util.Optional;
public interface WebhookEndpointRepository {
    WebhookEndpoint save(WebhookEndpoint endpoint);
    Optional<WebhookEndpoint> findById(Long id);
    List<WebhookEndpoint> findByTenantId(Long tenantId);
    List<WebhookEndpoint> findByTenantIdAndActive(Long tenantId, boolean active);
    List<WebhookEndpoint> findAllActive();
    void deleteById(Long id);
    boolean existsById(Long id);
}
