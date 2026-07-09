package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.DecisionConditionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "decision_rules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class DecisionRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "condition_type", nullable = false, length = 30) private DecisionConditionType conditionType;
    @Column(precision = 5, scale = 2) private BigDecimal threshold;
    @Column(name = "subject_id") private Long subjectId;
    @Column(name = "min_count") private Integer minCount;
    @Column(nullable = false, length = 30) private String decision;
    @Column(nullable = false) private int priority;
    @Column(nullable = false) private boolean active;
    @Column(name = "valid_from") private LocalDate validFrom;
    @Column(name = "valid_to") private LocalDate validTo;
}
