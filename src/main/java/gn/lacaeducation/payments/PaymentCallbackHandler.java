package gn.lacaeducation.payments;

/**
 * Published API for inbound provider callbacks (consumed by the {@code webhooks} module, §10.1).
 * Applies a verified provider callback to the payment behind a transaction: it records the raw
 * response on the transaction trace and drives the payment to SUCCEEDED or FAILED, which in turn
 * emits PaiementReussi / PaiementEchoue.
 */
public interface PaymentCallbackHandler {
    void applyCallback(Long transactionId, boolean success, String providerStatus, String rawPayload);
}
