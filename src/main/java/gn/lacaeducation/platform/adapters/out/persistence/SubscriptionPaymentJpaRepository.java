package gn.lacaeducation.platform.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface SubscriptionPaymentJpaRepository extends JpaRepository<SubscriptionPaymentJpaEntity, Long> {
    List<SubscriptionPaymentJpaEntity> findByInvoiceId(Long invoiceId);
    Optional<SubscriptionPaymentJpaEntity> findByExternalTransactionId(String externalTransactionId);
}
