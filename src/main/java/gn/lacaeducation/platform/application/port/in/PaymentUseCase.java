package gn.lacaeducation.platform.application.port.in;

import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentType;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import gn.lacaeducation.platform.domain.SubscriptionReceipt;

import java.math.BigDecimal;

public interface PaymentUseCase {
    SubscriptionPayment initiate(InitiatePaymentCommand command);
    SubscriptionPayment confirm(Long paymentId, String externalId);
    SubscriptionPayment fail(Long paymentId);
    SubscriptionPayment findPaymentById(Long id);
    SubscriptionReceipt findReceiptById(Long id);
    String generateReceiptPdf(Long receiptId);

    record InitiatePaymentCommand(
        Long invoiceId,
        BigDecimal amount,
        PaymentChannel channel,
        PaymentType paymentType
    ) {}
}
