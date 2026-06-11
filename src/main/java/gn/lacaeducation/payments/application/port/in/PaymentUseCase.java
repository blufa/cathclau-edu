package gn.lacaeducation.payments.application.port.in;
import gn.lacaeducation.payments.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface PaymentUseCase {
    record InitiatePaymentCommand(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type) {}
    Payment initiate(InitiatePaymentCommand command);
    Payment confirmSuccess(Long paymentId);
    Payment markFailed(Long paymentId);
    Payment markRefunded(Long paymentId);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByReference(String reference);
    List<Payment> findByInvoiceId(Long invoiceId);
    List<Payment> findByStudentId(Long studentId);
    List<Payment> findByTenantId(Long tenantId);
    List<Payment> findByTenantIdAndStatus(Long tenantId, PaymentStatus status);
    void delete(Long id);
}
