package gn.lacaeducation.webhooks.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface WebhookEndpointJpaRepository extends JpaRepository<WebhookEndpointJpaEntity, Long> {
    List<WebhookEndpointJpaEntity> findByTenantId(Long tenantId);
    List<WebhookEndpointJpaEntity> findByTenantIdAndActive(Long tenantId, boolean active);
}
