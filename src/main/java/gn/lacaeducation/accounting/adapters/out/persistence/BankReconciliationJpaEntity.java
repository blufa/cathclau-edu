package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "bank_reconciliations")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BankReconciliationJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "treasury_account_id", nullable = false) private Long treasuryAccountId;
    @Column(name = "statement_date", nullable = false) private LocalDate statementDate;
    @Column(name = "statement_balance", nullable = false, precision = 15, scale = 2) private BigDecimal statementBalance;
    @Column(name = "book_balance", nullable = false, precision = 15, scale = 2) private BigDecimal bookBalance;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ReconciliationStatus status;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "reconciled_by") private Long reconciledBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
