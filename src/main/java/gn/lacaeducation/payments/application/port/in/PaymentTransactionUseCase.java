package gn.lacaeducation.payments.application.port.in;
import gn.lacaeducation.payments.domain.*;
import java.util.List;
import java.util.Optional;
public interface PaymentTransactionUseCase {
    record RecordTransactionCommand(Long paymentId, PaymentProvider provider, String externalTransactionId, String providerStatus, String responsePayload) {}
    PaymentTransaction record(RecordTransactionCommand command);
    Optional<PaymentTransaction> findById(Long id);
    List<PaymentTransaction> findByPaymentId(Long paymentId);
    Optional<PaymentTransaction> findByExternalId(String externalId);
}
