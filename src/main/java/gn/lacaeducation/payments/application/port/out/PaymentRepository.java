package gn.lacaeducation.payments.application.port.out;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByTenantId(Long tenantId);
    List<Payment> findByTenantIdAndStatus(Long tenantId, PaymentStatus status);
    List<Payment> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    /** Paiements encaissés par un comptable précis (CASH uniquement — collectedByUserId n'est renseigné
     * que pour ce canal) sur une plage de dates — rapport par comptable + rapprochement de caisse. */
    List<Payment> findByCollectedByUserIdAndPaidAtBetween(Long collectedByUserId, LocalDateTime from, LocalDateTime to);
    void deleteById(Long id);
}
