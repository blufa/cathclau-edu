package gn.lacaeducation.payments.application.port.out;

/** Outbound port for Djomy server-side verification (signature + verify_payment). */
public interface DjomyVerificationPort {

    /** True when the "v1:&lt;hex&gt;" webhook signature matches HMAC-SHA256(rawBody, clientSecret). */
    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    /** Calls Djomy verify_payment and returns true only when the server-side status is SUCCESS. */
    boolean isConfirmedSuccess(String externalTransactionId);
}
