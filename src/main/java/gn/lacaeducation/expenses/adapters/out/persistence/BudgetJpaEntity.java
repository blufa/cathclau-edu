package gn.lacaeducation.expenses.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "expense_budget", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id","category_id","academic_year"}))
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class BudgetJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "category_id", nullable = false) private Long categoryId;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Column(name = "allocated_amount", nullable = false, precision = 15, scale = 2) private BigDecimal allocatedAmount;
}
