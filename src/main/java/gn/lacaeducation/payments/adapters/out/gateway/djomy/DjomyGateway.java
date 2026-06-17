package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.application.port.out.PaymentProviderGateway;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.PaymentProvider;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Djomy aggregator adapter behind the {@link PaymentProviderGateway} port (P9), added alongside
 * the existing providers without removing any. Resolves for {@link PaymentChannel#DJOMY}:
 * create_payment_gateway returns a redirectUrl that the frontend uses to send the payer to the
 * Djomy portal. {@code phoneOrCard} carries the mandatory payer phone number.
 */
@Component
@RequiredArgsConstructor
public class DjomyGateway implements PaymentProviderGateway {

    private final DjomyClient client;

    @Override
    public PaymentChannel getChannel() {
        return PaymentChannel.DJOMY;
    }

    @Override
    public PaymentTransaction initiate(Long paymentId, BigDecimal amount, String reference, String phoneOrCard) throws JsonProcessingException{
        DjomyClient.GatewayResult result = client.createPaymentGateway(phoneOrCard, amount, reference, null);
        return PaymentTransaction.builder()
                .paymentId(paymentId)
                .provider(PaymentProvider.DJOMY)
                .externalTransactionId(result.transactionId())
                .providerStatus("PENDING")
                .redirectUrl(result.redirectUrl())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean verifySignature(String payload, String signature, String secret) {
        return DjomySignature.verifyWebhook(payload, signature, secret);
    }
}
