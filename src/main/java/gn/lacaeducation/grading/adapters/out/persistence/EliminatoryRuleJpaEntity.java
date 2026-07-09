package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "eliminatory_rules")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class EliminatoryRuleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "subject_id", nullable = false) private Long subjectId;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal threshold;
    @Column(name = "decision_label", nullable = false, length = 30) private String decisionLabel;
    @Column(nullable = false) private boolean active;
}
