package gn.lacaeducation.payments.application.port.in;
import gn.lacaeducation.payments.domain.Receipt;
import java.util.List;
import java.util.Optional;
public interface ReceiptUseCase {
    Receipt generate(Long paymentId);
    Optional<Receipt> findById(Long id);
    Optional<Receipt> findByPaymentId(Long paymentId);
    Optional<Receipt> findByReference(String reference);
    List<Receipt> findByStudentId(Long studentId);
}
