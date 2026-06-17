package gn.lacaeducation.payments.application.port.out;
import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import java.math.BigDecimal;
public interface PaymentProviderGateway {
    PaymentChannel getChannel();
    PaymentTransaction initiate(Long paymentId, BigDecimal amount, String reference, String phoneOrCard) throws JsonProcessingException;
    boolean verifySignature(String payload, String signature, String secret);
}
