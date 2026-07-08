package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "assessment_type_configs")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class AssessmentTypeConfigJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(nullable = false, length = 30) private String code;
    @Column(nullable = false) private String label;
    @Column(name = "default_coefficient", precision = 5, scale = 2) private BigDecimal defaultCoefficient;
    @Column(precision = 5, scale = 2) private BigDecimal weight;
    @Column(length = 20) private String color;
    @Column(nullable = false) private boolean active;
}
