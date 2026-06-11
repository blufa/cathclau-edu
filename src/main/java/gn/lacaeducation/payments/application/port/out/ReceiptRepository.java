package gn.lacaeducation.payments.application.port.out;
import gn.lacaeducation.payments.domain.Receipt;
import java.util.Optional;
public interface ReceiptRepository {
    Receipt save(Receipt receipt);
    Optional<Receipt> findById(Long id);
    Optional<Receipt> findByPaymentId(Long paymentId);
    Optional<Receipt> findByReference(String reference);
}
