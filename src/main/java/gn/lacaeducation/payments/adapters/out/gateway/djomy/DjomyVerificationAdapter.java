package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.application.port.out.DjomyVerificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Djomy implementation of the verification port: webhook signature + verify_payment. */
@Component
@RequiredArgsConstructor
class DjomyVerificationAdapter implements DjomyVerificationPort {

    private final DjomyClient client;
    private final DjomyProperties props;

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        return DjomySignature.verifyWebhook(rawBody, signatureHeader, props.clientSecret());
    }

    @Override
    public DjomyStatus verify(String externalTransactionId) throws JsonProcessingException {
        DjomyClient.Verification v = client.verifyPayment(externalTransactionId);
        return new DjomyStatus(v.status(), v.receivedAmount(), v.currency(), v.isSuccess());
    }

    @Override
    public boolean isConfirmedSuccess(String externalTransactionId) throws JsonProcessingException {
        return client.verifyPayment(externalTransactionId).isSuccess();
    }
}
