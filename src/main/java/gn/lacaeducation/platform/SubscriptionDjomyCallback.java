package gn.lacaeducation.platform;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Published API (consumed by the {@code webhooks} module) to apply a Djomy callback to a
 * subscription payment. Lets the single Djomy webhook endpoint route to the subscription flow
 * when the transaction does not belong to a school payment.
 */
public interface SubscriptionDjomyCallback {

    /** True when the given Djomy transaction id belongs to a subscription payment. */
    boolean handles(String externalTransactionId);

    /** Confirms server-side via verify_payment, then activates the subscription. Returns confirmed. */
    boolean confirmSuccess(String externalTransactionId) throws JsonProcessingException;

    /** Marks the subscription payment as failed. */
    void markFailed(String externalTransactionId);
}
