package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.PayslipLineType;
import gn.lacaeducation.payroll.domain.RuleComputation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "salary_rules")
@Getter
@Setter
@org.hibernate.annotations.Filter(name = "tenantFilter")
public class SalaryRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "structure_id", nullable = false) private Long structureId;
    @Column(nullable = false) private String label;
    @Column(length = 32) private String code;
    @Column(length = 32) private String category;
    @Enumerated(EnumType.STRING) @Column(name = "line_type", nullable = false, length = 16) private PayslipLineType lineType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private RuleComputation computation;
    @Column(precision = 15, scale = 4) private BigDecimal value;
    @Column(nullable = false) private Integer sequence;
}
