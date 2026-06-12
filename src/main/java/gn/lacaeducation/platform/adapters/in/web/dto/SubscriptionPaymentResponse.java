package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.PaymentType;
import gn.lacaeducation.platform.domain.SubscriptionPayment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionPaymentResponse(
        Long id,
        Long invoiceId,
        String reference,
        BigDecimal amount,
        PaymentChannel channel,
        PaymentType paymentType,
        PaymentStatus status,
        String externalTransactionId,
        LocalDateTime paidAt
) {
    public static SubscriptionPaymentResponse from(SubscriptionPayment p) {
        return new SubscriptionPaymentResponse(p.getId(), p.getInvoiceId(), p.getReference(),
                p.getAmount(), p.getChannel(), p.getPaymentType(), p.getStatus(),
                p.getExternalTransactionId(), p.getPaidAt());
    }
}
