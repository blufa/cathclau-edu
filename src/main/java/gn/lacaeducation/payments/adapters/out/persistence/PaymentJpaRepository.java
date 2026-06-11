package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByReference(String reference);
    List<PaymentJpaEntity> findByInvoiceId(Long invoiceId);
    List<PaymentJpaEntity> findByStudentId(Long studentId);
    List<PaymentJpaEntity> findByTenantId(Long tenantId);
    List<PaymentJpaEntity> findByTenantIdAndStatus(Long tenantId, PaymentStatus status);
}
