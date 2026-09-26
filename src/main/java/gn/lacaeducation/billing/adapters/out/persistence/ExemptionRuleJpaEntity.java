package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "exemption_rules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class ExemptionRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ExemptionType type;
    @Column(precision = 5, scale = 2) private BigDecimal percentage;
    @Column(precision = 15, scale = 2) private BigDecimal amount;
    @Convert(converter = FeeTypeListConverter.class) @Column(name = "fee_types") private List<FeeType> feeTypes;
    @Column(name = "staff_benefit", nullable = false) private boolean staffBenefit;
    @Column(nullable = false) private boolean active;
}
