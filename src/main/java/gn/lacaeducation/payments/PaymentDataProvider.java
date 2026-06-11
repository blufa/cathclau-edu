package gn.lacaeducation.payments;
import java.util.Optional;
public interface PaymentDataProvider {
    Optional<PaymentPublicInfo> findPayment(Long paymentId);
}
