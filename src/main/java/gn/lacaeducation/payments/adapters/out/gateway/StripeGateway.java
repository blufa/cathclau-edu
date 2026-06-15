package gn.lacaeducation.payments.adapters.out.gateway;
import gn.lacaeducation.payments.application.port.out.PaymentProviderGateway;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.PaymentProvider;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Component
public class StripeGateway implements PaymentProviderGateway {
    @Override public PaymentChannel getChannel() { return PaymentChannel.BANK_CARD; }
    @Override public PaymentTransaction initiate(Long paymentId, BigDecimal amount, String reference, String phoneOrCard) {
        return PaymentTransaction.builder().paymentId(paymentId).provider(PaymentProvider.STRIPE)
                .externalTransactionId("STRIPE-" + reference).providerStatus("PENDING")
                .createdAt(LocalDateTime.now()).build();
    }
    @Override public boolean verifySignature(String payload, String signature, String secret) {
        return signature != null && !signature.isBlank();
    }
}
