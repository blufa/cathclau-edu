package gn.lacaeducation.platform.application.port.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentType;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import gn.lacaeducation.platform.domain.SubscriptionReceipt;

import java.math.BigDecimal;

public interface PaymentUseCase {
    SubscriptionPayment initiate(InitiatePaymentCommand command)throws JsonProcessingException;
    SubscriptionPayment confirm(Long paymentId, String externalId);
    SubscriptionPayment fail(Long paymentId);
    SubscriptionPayment findPaymentById(Long id);
    SubscriptionReceipt findReceiptById(Long id);
    String generateReceiptPdf(Long receiptId);

    /** On-demand query of the live Djomy status for a payment (diagnostic; webhook stays authoritative). */
    DjomyStatusResponse checkDjomyStatus(Long paymentId) throws JsonProcessingException;

    record DjomyStatusResponse(
        Long paymentId,
        String externalTransactionId,
        String djomyStatus,
        BigDecimal amount,
        String currency,
        boolean success
    ) {}

    record InitiatePaymentCommand(
        Long invoiceId,
        BigDecimal amount,
        PaymentChannel channel,
        PaymentType paymentType,
        /** Payer phone number — required by aggregator channels (Djomy). */
        String payerNumber
    ) {}
}
