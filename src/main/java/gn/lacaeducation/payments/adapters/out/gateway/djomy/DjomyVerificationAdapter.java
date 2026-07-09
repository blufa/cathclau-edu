package gn.lacaeducation.payments.adapters.out.gateway.djomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.payments.application.port.out.DjomyVerificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Djomy implementation of the verification port: webhook signature + verify_payment. */
@Component
@RequiredArgsConstructor
@Slf4j
class DjomyVerificationAdapter implements DjomyVerificationPort {

    private final DjomyClient client;
    private final DjomyProperties props;

    @Override
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        String secret = props.webhookSecretOrClientSecret();
        boolean ok = DjomySignature.verifyWebhook(rawBody, signatureHeader, secret);
        if (!ok && signatureHeader != null && signatureHeader.startsWith("v1:") && rawBody != null) {
            // TEMP diagnostic: find which body form Djomy signs (raw / stripped / no-whitespace).
            String provided = signatureHeader.substring(3).trim().toLowerCase();
            String body = rawBody;
            String compact = rawBody.replaceAll("\\s+", "");
            log.warn("Djomy sig debug: provided={} | rawMatch={} strippedMatch={} compactMatch={} | bodyLen={} secretLen={}",
                    provided,
                    DjomySignature.hmacHex(body, secret).equals(provided),
                    DjomySignature.hmacHex(body.strip(), secret).equals(provided),
                    DjomySignature.hmacHex(compact, secret).equals(provided),
                    rawBody.length(), secret == null ? 0 : secret.length());
        }
        return ok;
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
