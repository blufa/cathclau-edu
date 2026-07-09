package gn.lacaeducation.payments;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Optional;

/**
 * Published API (consumed by the {@code webhooks} module, §10.1) for the Djomy aggregator callback.
 * Keeps the Djomy secret + verify_payment confirmation inside the payments module: the webhook
 * adapter only verifies the signature, parses the payload and delegates fulfillment here.
 */
public interface DjomyCallbackHandler {

    /** Verifies a Djomy webhook "X-Webhook-Signature: v1:&lt;hex&gt;" header against the raw body. */
    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    /** Resolves the local transaction id behind a Djomy transaction id (for audit storage). */
    Optional<Long> resolveTransactionId(String externalTransactionId);

    /**
     * payment.success path: confirms server-side via Djomy verify_payment (never trusts the webhook
     * alone) and, only if confirmed SUCCESS, drives the payment to SUCCEEDED. Returns whether confirmed.
     */
    boolean confirmSuccess(String externalTransactionId) throws JsonProcessingException;

    /** payment.failed path: drives the matching payment to FAILED. */
    void markFailed(String externalTransactionId);
}
