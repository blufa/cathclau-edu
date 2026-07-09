package gn.lacaeducation.grants.adapters.out.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity @Table(name = "grant_payments")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class GrantPaymentJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "grant_id", nullable = false) private Long grantId;
    @Column(name = "payment_date", nullable = false) private LocalDate paymentDate;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal amount;
    @Column(length = 40) private String method;
    @Column(length = 80) private String reference;
    @Column(name = "treasury_account_id") private Long treasuryAccountId;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
