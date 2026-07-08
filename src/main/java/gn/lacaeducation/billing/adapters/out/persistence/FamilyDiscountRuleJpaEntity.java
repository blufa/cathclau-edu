package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "family_discount_rules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class FamilyDiscountRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(name = "min_children", nullable = false) private Integer minChildren;
    @Column(name = "max_children") private Integer maxChildren;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal percentage;
    @Enumerated(EnumType.STRING) @Column(name = "fee_type", length = 20) private FeeType feeType;
    @Column(name = "applies_to_youngest", nullable = false) private boolean appliesToYoungest;
    @Column(nullable = false) private boolean active;
}
