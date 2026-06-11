package gn.lacaeducation.webhooks.application.port.in;
import gn.lacaeducation.webhooks.domain.WebhookEndpoint;
import java.util.List;
import java.util.Optional;
public interface WebhookEndpointUseCase {
    record CreateEndpointCommand(Long tenantId, String url, String secret, String events) {}
    record UpdateEndpointCommand(String url, String secret, String events, Boolean active) {}
    WebhookEndpoint create(CreateEndpointCommand command);
    WebhookEndpoint update(Long id, UpdateEndpointCommand command);
    Optional<WebhookEndpoint> findById(Long id);
    List<WebhookEndpoint> findByTenantId(Long tenantId);
    List<WebhookEndpoint> findActiveByTenantId(Long tenantId);
    void activate(Long id);
    void deactivate(Long id);
    void delete(Long id);
}
