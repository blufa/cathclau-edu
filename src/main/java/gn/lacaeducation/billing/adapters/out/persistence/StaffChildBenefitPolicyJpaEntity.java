package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "staff_child_benefit_policies")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class StaffChildBenefitPolicyJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(name = "covered_children_count", nullable = false) private Integer coveredChildrenCount;
    @Column(name = "coverage_percent", nullable = false, precision = 5, scale = 2) private BigDecimal coveragePercent;
    @Convert(converter = FeeTypeListConverter.class) @Column(name = "fee_types") private List<FeeType> feeTypes;
    @Column(nullable = false) private boolean active;
}
