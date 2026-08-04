package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.domain.BudgetLineType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "budget_lines")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BudgetLineJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "budget_id", nullable = false) private Long budgetId;
    @Enumerated(EnumType.STRING) @Column(name = "line_type", nullable = false, length = 8) private BudgetLineType lineType;
    @Column(nullable = false, length = 160) private String category;
    @Column private Integer quantity;
    @Column(name = "unit_price", precision = 15, scale = 2) private BigDecimal unitPrice;
    @Column(name = "planned_amount", nullable = false, precision = 15, scale = 2) private BigDecimal plannedAmount;
    @Column(name = "actual_amount", nullable = false, precision = 15, scale = 2) private BigDecimal actualAmount;
    @Column(columnDefinition = "text") private String notes;
}
