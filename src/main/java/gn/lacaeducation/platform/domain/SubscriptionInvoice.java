package gn.lacaeducation.platform.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionInvoice {
    private Long id;
    private Long subscriptionId;
    private String reference;
    private BillingPeriod billingPeriod;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal netAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingBalance;
    private InvoiceStatus status;
    private LocalDate issuedAt;
    private LocalDate dueDate;
    private String pdfFile;

    public boolean isPaid() {
        return remainingBalance != null && remainingBalance.compareTo(BigDecimal.ZERO) == 0;
    }
}
