package gn.lacaeducation.payroll.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_fee_deduction_rules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class SalaryFeeDeductionRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(name = "amount_per_child", nullable = false, precision = 12, scale = 2) private BigDecimal amountPerChild;
    @Column(name = "max_children") private Integer maxChildren;
    @Column(precision = 12, scale = 2) private BigDecimal cap;
    @Column(nullable = false) private boolean active;
}
