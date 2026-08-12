package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "early_payment_discount_rules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class EarlyPaymentDiscountRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal percentage;
    @Enumerated(EnumType.STRING) @Column(name = "fee_type", length = 20) private FeeType feeType;
    @Column(nullable = false) private boolean active;
}
