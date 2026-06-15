package gn.lacaeducation.payments.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionJpaEntity, Long> {
    List<PaymentTransactionJpaEntity> findByPaymentId(Long paymentId);
    List<PaymentTransactionJpaEntity> findByProvider(gn.lacaeducation.payments.domain.PaymentProvider provider);
    Optional<PaymentTransactionJpaEntity> findByExternalTransactionId(String externalId);
}
