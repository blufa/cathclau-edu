package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.domain.CashierSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "cashier_sessions")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class CashierSessionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "cashier_id", nullable = false) private Long cashierId;
    @Column(name = "opened_at", nullable = false) private LocalDateTime openedAt;
    @Column(name = "closed_at") private LocalDateTime closedAt;
    @Column(name = "system_total", precision = 15, scale = 2) private BigDecimal systemTotal;
    @Column(name = "physical_count", precision = 15, scale = 2) private BigDecimal physicalCount;
    @Column(name = "discrepancy", precision = 15, scale = 2) private BigDecimal discrepancy;
    @Column(columnDefinition = "text") private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CashierSessionStatus status;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
