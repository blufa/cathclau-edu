package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.AverageMethod;
import gn.lacaeducation.grading.domain.RoundingPolicy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grading_calculation_settings")
@Getter @Setter @NoArgsConstructor
class GradingCalculationSettingsJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, unique = true) private Long tenantId;
    @Enumerated(EnumType.STRING) @Column(name = "average_method", nullable = false, length = 20) private AverageMethod averageMethod;
    @Column(nullable = false) private Integer decimals;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RoundingPolicy rounding;
}
