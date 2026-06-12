package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "grading_scales")
@Getter @Setter @NoArgsConstructor
class GradingScaleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false, unique = true) private Long tenantId;
    @Column(name = "pass_threshold", nullable = false) private BigDecimal passThreshold;
    @Column(name = "passable_min", nullable = false) private BigDecimal passableMin;
    @Column(name = "assez_bien_min", nullable = false) private BigDecimal assezBienMin;
    @Column(name = "bien_min", nullable = false) private BigDecimal bienMin;
    @Column(name = "tres_bien_min", nullable = false) private BigDecimal tresBienMin;
}
