package gn.lacaeducation.accounting.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
@Entity @Table(name = "accounting_journal") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class JournalJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String code;
    private String label;
    private String type;
    @Column(name = "default_counterpart_account_id") private Long defaultCounterpartAccountId;
}
