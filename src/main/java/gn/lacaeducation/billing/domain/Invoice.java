package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Invoice {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long feeId;
    private String reference;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingBalance;
    private InvoiceStatus status;
    private LocalDate issueDate;
}
