package gn.lacaeducation.payments;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigDecimal;

/**
 * Published API exposing the Djomy aggregator gateway primitives to other modules (e.g. platform
 * subscription payments), without leaking the Djomy HTTP client / secret. Both school and
 * subscription flows reuse the same Djomy integration through this facade.
 */
public interface DjomyGatewayApi {

    record GatewayResult(String transactionId, String redirectUrl) {}

    /** Live Djomy status of a transaction (status as CREATED/PENDING/SUCCESS/…, net amount, currency). */
    record DjomyStatus(String status, BigDecimal amount, String currency, boolean success) {}

    /** create_payment_gateway: returns the Djomy transaction id + the portal redirect URL. */
    GatewayResult createGatewayPayment(String payerNumber, BigDecimal amount, String reference, String description) throws JsonProcessingException;

    /** verify_payment, server-side: the live Djomy status of the transaction. */
    DjomyStatus verify(String externalTransactionId) throws JsonProcessingException;

    /** verify_payment, server-side: true only when Djomy reports status SUCCESS. */
    boolean isConfirmedSuccess(String externalTransactionId) throws JsonProcessingException;
}
