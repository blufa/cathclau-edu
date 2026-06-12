package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name = "invoices") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class InvoiceJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "student_id", nullable = false) private Long studentId;
    @Column(name = "fee_id") private Long feeId;
    @Column(unique = true, nullable = false) private String reference;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2) private BigDecimal totalAmount;
    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2) private BigDecimal paidAmount;
    @Column(name = "remaining_balance", nullable = false, precision = 15, scale = 2) private BigDecimal remainingBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private InvoiceStatus status;
    @Column(name = "issue_date", nullable = false) private LocalDate issueDate;
}
