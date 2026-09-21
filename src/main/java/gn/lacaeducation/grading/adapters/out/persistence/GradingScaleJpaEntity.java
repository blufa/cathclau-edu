package gn.lacaeducation.grading.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "grading_scales",
        uniqueConstraints = @UniqueConstraint(name = "uq_grading_scales_tenant_cycle",
                columnNames = {"tenant_id", "cycle_group"}))
@Getter @Setter @NoArgsConstructor
class GradingScaleJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "cycle_group", nullable = false) private String cycleGroup;
    @Column(name = "pass_threshold", nullable = false) private BigDecimal passThreshold;
    @Column(name = "passable_min", nullable = false) private BigDecimal passableMin;
    @Column(name = "assez_bien_min", nullable = false) private BigDecimal assezBienMin;
    @Column(name = "bien_min", nullable = false) private BigDecimal bienMin;
    @Column(name = "tres_bien_min", nullable = false) private BigDecimal tresBienMin;
    @Column(name = "felicitations_min") private BigDecimal felicitationsMin;
    @Column(name = "compliments_min") private BigDecimal complimentsMin;
    @Column(name = "distinction_no_failing_threshold") private BigDecimal distinctionNoFailingThreshold;
}
