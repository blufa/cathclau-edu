package gn.lacaeducation.accounting.adapters.out.persistence;
import gn.lacaeducation.accounting.domain.AccountingMode;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name = "accounting_settings") @Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class AccountingSettingsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AccountingMode mode;
    private String referential;
    @Column(length = 8) private String currency;
    @Column(name = "fiscal_year_start_month") private Integer fiscalYearStartMonth;
    @Column(name = "vat_applicable") private Boolean vatApplicable;
    @Column(name = "default_vat_rate", precision = 6, scale = 2) private BigDecimal defaultVatRate;
    @Column(name = "auto_numbering") private Boolean autoNumbering;
    private Boolean active;
}
