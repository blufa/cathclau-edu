package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.AccountType;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
@Entity @Table(name = "chart_of_account") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class ChartOfAccountJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "account_number", nullable = false) private String accountNumber;
    @Column(nullable = false) private String label;
    @Column(name = "account_class") private Integer accountClass;
    @Enumerated(EnumType.STRING) @Column(length = 12) private AccountType type;
    @Column(name = "parent_id") private Long parentId;
    private Boolean lettrable;
    private Boolean active;
}
