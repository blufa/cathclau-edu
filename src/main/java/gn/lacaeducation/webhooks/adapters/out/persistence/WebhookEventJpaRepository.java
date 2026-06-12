package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.domain.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
interface WebhookEventJpaRepository extends JpaRepository<WebhookEventJpaEntity, Long> {
    List<WebhookEventJpaEntity> findByEndpointId(Long endpointId);
    List<WebhookEventJpaEntity> findByStatus(WebhookDeliveryStatus status);
    List<WebhookEventJpaEntity> findByStatusAndNextAttemptAtBefore(WebhookDeliveryStatus status, LocalDateTime cutoff);
}
