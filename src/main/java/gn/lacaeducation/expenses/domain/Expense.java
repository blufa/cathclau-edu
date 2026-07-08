package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A school expense following the approval workflow DRAFT → SUBMITTED → APPROVED/REJECTED → PAID. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private Long supplierId;
    private String label;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private ExpensePaymentMode paymentMode;
    private String reference;
    private String justificationUrl;
    private ExpenseStatus status;
    private String rejectionReason;
    private Long decidedBy;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
}
