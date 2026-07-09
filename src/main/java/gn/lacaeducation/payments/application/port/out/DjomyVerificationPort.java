package gn.lacaeducation.payments.application.port.out;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.math.BigDecimal;

/** Outbound port for Djomy server-side verification (signature + verify_payment). */
public interface DjomyVerificationPort {

    /** Live Djomy status of a transaction (status as CREATED/PENDING/SUCCESS/…, net amount, currency). */
    record DjomyStatus(String status, BigDecimal amount, String currency, boolean success) {}

    /** True when the "v1:&lt;hex&gt;" webhook signature matches HMAC-SHA256(rawBody, clientSecret). */
    boolean verifyWebhookSignature(String rawBody, String signatureHeader);

    /** Calls Djomy verify_payment and returns the live status of the transaction. */
    DjomyStatus verify(String externalTransactionId) throws JsonProcessingException;

    /** Calls Djomy verify_payment and returns true only when the server-side status is SUCCESS. */
    boolean isConfirmedSuccess(String externalTransactionId) throws JsonProcessingException;
}
