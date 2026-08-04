package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.domain.ExpensePaymentMode;
import gn.lacaeducation.expenses.domain.ExpenseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "expense") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ExpenseJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "category_id") private Long categoryId;
    @Column(name = "supplier_id") private Long supplierId;
    @Column(name = "budget_line_id") private Long budgetLineId;
    @Column(nullable = false) private String label;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 8) private String currency;
    @Column(name = "expense_date") private LocalDate expenseDate;
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", length = 16) private ExpensePaymentMode paymentMode;
    private String reference;
    @Column(name = "justification_url") private String justificationUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ExpenseStatus status;
    @Column(name = "rejection_reason") private String rejectionReason;
    @Column(name = "decided_by") private Long decidedBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "paid_at") private LocalDateTime paidAt;
}
