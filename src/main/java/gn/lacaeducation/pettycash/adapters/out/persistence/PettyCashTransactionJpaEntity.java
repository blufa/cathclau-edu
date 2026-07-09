package gn.lacaeducation.pettycash.adapters.out.persistence;
import gn.lacaeducation.pettycash.domain.PettyCashTxType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "petty_cash_transactions")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class PettyCashTransactionJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "fund_id", nullable = false) private Long fundId;
    @Column(name = "tx_date", nullable = false) private LocalDate txDate;
    @Enumerated(EnumType.STRING) @Column(name = "tx_type", nullable = false, length = 16) private PettyCashTxType txType;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(name = "voucher_number", length = 60) private String voucherNumber;
    @Column(columnDefinition = "text") private String description;
    @Column(length = 120) private String category;
    @Column(length = 160) private String recipient;
    @Column(name = "justification_url", length = 1024) private String justificationUrl;
    @Column(name = "recorded_by") private Long recordedBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
