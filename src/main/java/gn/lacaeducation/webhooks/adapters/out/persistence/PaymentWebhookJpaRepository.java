package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.domain.WebhookProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface PaymentWebhookJpaRepository extends JpaRepository<PaymentWebhookJpaEntity, Long> {
    List<PaymentWebhookJpaEntity> findByProcessed(boolean processed);
    List<PaymentWebhookJpaEntity> findByProvider(WebhookProvider provider);
    boolean existsByExternalEventId(String externalEventId);
}
