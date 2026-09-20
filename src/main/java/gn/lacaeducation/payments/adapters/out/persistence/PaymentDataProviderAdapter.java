package gn.lacaeducation.payments.adapters.out.persistence;
import gn.lacaeducation.payments.PaymentDataProvider;
import gn.lacaeducation.payments.PaymentPublicInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
public class PaymentDataProviderAdapter implements PaymentDataProvider {
    private final PaymentJpaRepository jpa;
    @Override
    public Optional<PaymentPublicInfo> findPayment(Long paymentId) {
        return jpa.findById(paymentId).map(e ->
                new PaymentPublicInfo(e.getId(), e.getReference(), e.getAmount(), e.getChannel().name(),
                        e.getPaidAt(), e.getCoveredMonths()));
    }
}
