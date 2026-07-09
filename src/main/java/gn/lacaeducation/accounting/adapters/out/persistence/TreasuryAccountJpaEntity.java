package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.TreasuryAccountType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "treasury_account") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class TreasuryAccountJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private TreasuryAccountType type;
    private String bank;
    @Column(name = "account_number") private String accountNumber;
    @Column(name = "initial_balance", precision = 15, scale = 2) private BigDecimal initialBalance;
    @Column(name = "current_balance", precision = 15, scale = 2) private BigDecimal currentBalance;
    @Column(length = 8) private String currency;
    private Boolean active;
}
