package gn.lacaeducation.platform.adapters.in.web.dto;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;
import gn.lacaeducation.platform.domain.InvoiceStatus;
import gn.lacaeducation.platform.domain.BillingPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
public record SubscriptionInvoiceResponse(Long id, Long subscriptionId, String reference,
        BillingPeriod billingPeriod, BigDecimal netAmount, BigDecimal taxAmount,
        BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingBalance,
        InvoiceStatus status, LocalDate issuedAt, LocalDate dueDate) {
    public static SubscriptionInvoiceResponse from(SubscriptionInvoice i) {
        return new SubscriptionInvoiceResponse(i.getId(), i.getSubscriptionId(), i.getReference(),
                i.getBillingPeriod(), i.getNetAmount(), i.getTaxAmount(), i.getTotalAmount(),
                i.getPaidAmount(), i.getRemainingBalance(), i.getStatus(), i.getIssuedAt(), i.getDueDate());
    }
}
