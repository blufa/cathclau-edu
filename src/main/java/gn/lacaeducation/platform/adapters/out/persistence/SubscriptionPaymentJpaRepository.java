package gn.lacaeducation.platform.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface SubscriptionPaymentJpaRepository extends JpaRepository<SubscriptionPaymentJpaEntity, Long> {
    List<SubscriptionPaymentJpaEntity> findByInvoiceId(Long invoiceId);
}
