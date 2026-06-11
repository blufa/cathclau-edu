package gn.lacaeducation.platform.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface SubscriptionInvoiceJpaRepository extends JpaRepository<SubscriptionInvoiceJpaEntity, Long> {
    List<SubscriptionInvoiceJpaEntity> findBySubscriptionId(Long subscriptionId);
    Optional<SubscriptionInvoiceJpaEntity> findTopBySubscriptionIdOrderByIssuedAtDesc(Long subscriptionId);
}
