package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.domain.FinancingMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "monthly_expense_sheet") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class MonthlyExpenseSheetJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(name = "academic_year", nullable = false, length = 20) private String academicYear;
    @Enumerated(EnumType.STRING) @Column(name = "financing_mode", length = 16) private FinancingMode financingMode;
    @Column(name = "financing_amount", precision = 15, scale = 2) private BigDecimal financingAmount;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "created_by") private Long createdBy;
    @Column(name = "closed_at") private LocalDateTime closedAt;
}
