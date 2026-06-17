package gn.lacaeducation.payments.application.port.in;
import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface PaymentUseCase {
    record InitiatePaymentCommand(Long tenantId, Long invoiceId, Long studentId, String reference, BigDecimal amount, PaymentChannel channel, PaymentType type,
                                  /** Payer phone number — required by aggregator channels (e.g. Djomy). */
                                  String payerNumber) {}
    Payment initiate(InitiatePaymentCommand command) throws JsonProcessingException;

    /** On-demand query of the live Djomy status for a payment (diagnostic; webhook stays authoritative). */
    DjomyStatusResponse checkDjomyStatus(Long paymentId) throws JsonProcessingException;

    record DjomyStatusResponse(Long paymentId, String externalTransactionId, String djomyStatus,
                               BigDecimal amount, String currency, boolean success) {}

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
