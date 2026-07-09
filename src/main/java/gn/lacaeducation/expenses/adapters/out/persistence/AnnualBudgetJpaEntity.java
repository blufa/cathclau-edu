package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.domain.BudgetStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "annual_budgets")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class AnnualBudgetJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "academic_year", nullable = false, length = 20) private String academicYear;
    @Column(nullable = false, length = 160) private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private BudgetStatus status;
    @Column(name = "approved_by") private Long approvedBy;
    @Column(name = "approved_at") private LocalDate approvedAt;
    @Column(name = "rejection_reason", columnDefinition = "text") private String rejectionReason;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
