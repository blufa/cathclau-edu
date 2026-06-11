package gn.lacaeducation.payments.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface ReceiptJpaRepository extends JpaRepository<ReceiptJpaEntity, Long> {
    Optional<ReceiptJpaEntity> findByPaymentId(Long paymentId);
    Optional<ReceiptJpaEntity> findByReference(String reference);
}
