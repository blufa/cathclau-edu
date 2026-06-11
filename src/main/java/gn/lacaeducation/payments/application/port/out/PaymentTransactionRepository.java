package gn.lacaeducation.payments.application.port.out;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import java.util.List;
import java.util.Optional;
public interface PaymentTransactionRepository {
    PaymentTransaction save(PaymentTransaction transaction);
    Optional<PaymentTransaction> findById(Long id);
    List<PaymentTransaction> findByPaymentId(Long paymentId);
    Optional<PaymentTransaction> findByExternalTransactionId(String externalId);
}
