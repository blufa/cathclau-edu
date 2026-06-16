package gn.lacaeducation.payments.adapters.out.gateway.djomy;

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
    public boolean isConfirmedSuccess(String externalTransactionId) {
        return client.verifyPayment(externalTransactionId).isSuccess();
    }
}
