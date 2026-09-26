package gn.lacaeducation.expenses.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "revenue_line") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class RevenueLineJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "sheet_id", nullable = false) private Long sheetId;
    @Column(nullable = false) private String label;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "revenue_date") private LocalDate revenueDate;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
