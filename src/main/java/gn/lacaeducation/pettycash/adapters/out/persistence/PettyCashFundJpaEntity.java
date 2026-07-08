package gn.lacaeducation.pettycash.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "petty_cash_funds")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class PettyCashFundJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 160) private String label;
    @Column(name = "float_amount", nullable = false, precision = 15, scale = 2) private BigDecimal floatAmount;
    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2) private BigDecimal currentBalance;
    @Column(length = 160) private String custodian;
    @Column(nullable = false) private Boolean active;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
