package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.CashFlowCategory;
import gn.lacaeducation.accounting.domain.CashFlowDirection;
import gn.lacaeducation.accounting.domain.CashFlowReferenceType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name = "cash_flow_entry") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class CashFlowEntryJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "account_id", nullable = false) private Long accountId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 4) private CashFlowDirection direction;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(length = 8) private String currency;
    private LocalDate date;
    @Enumerated(EnumType.STRING) @Column(length = 16) private CashFlowCategory category;
    private String label;
    @Enumerated(EnumType.STRING) @Column(name = "reference_type", length = 12) private CashFlowReferenceType referenceType;
    @Column(name = "reference_id") private Long referenceId;
    @Column(name = "justification_url") private String justificationUrl;
    private Boolean reconciled;
}
