package gn.lacaeducation.expenses.adapters.out.persistence;
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
    @Column(name = "sheet_id") private Long sheetId;
    @Column(nullable = false) private String label;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private boolean necessary;
    @Column(name = "expense_date") private LocalDate expenseDate;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
