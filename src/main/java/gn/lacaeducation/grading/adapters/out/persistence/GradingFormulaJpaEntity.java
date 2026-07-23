package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grading_formula")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class GradingFormulaJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false) private String label;
    @Column(name = "period_type", nullable = false, length = 30) private String periodType;
    @Column(nullable = false) private boolean active;
}
