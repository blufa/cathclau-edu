package gn.lacaeducation.grants.adapters.out.persistence;
import gn.lacaeducation.grants.domain.GrantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "grants")
@Getter @Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class GrantJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 200) private String source;
    @Column(length = 80) private String reference;
    @Column(columnDefinition = "text") private String purpose;
    @Column(name = "academic_year", length = 20) private String academicYear;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2) private BigDecimal totalAmount;
    @Column(name = "received_amount", nullable = false, precision = 15, scale = 2) private BigDecimal receivedAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private GrantStatus status;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at") private LocalDateTime createdAt;
}
