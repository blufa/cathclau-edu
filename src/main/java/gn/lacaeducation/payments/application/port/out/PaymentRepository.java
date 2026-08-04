package gn.lacaeducation.payments.application.port.out;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import java.util.List;
import java.util.Optional;
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByReference(String reference);
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByTenantId(Long tenantId);
    List<Payment> findByTenantIdAndStatus(Long tenantId, PaymentStatus status);
    List<Payment> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    void deleteById(Long id);
}
