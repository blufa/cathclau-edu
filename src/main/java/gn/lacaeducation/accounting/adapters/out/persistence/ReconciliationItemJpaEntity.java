package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.ReconciliationItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "reconciliation_items")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ReconciliationItemJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "reconciliation_id", nullable = false) private Long reconciliationId;
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false, length = 24) private ReconciliationItemType itemType;
    @Column(length = 255) private String description;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
}
