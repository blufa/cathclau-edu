package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.WeightMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "grading_formula_step_input")
@Getter @Setter @NoArgsConstructor
class GradingFormulaStepInputJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "step_id", nullable = false) private Long stepId;
    @Column(name = "ref_step_order", nullable = false) private int refStepOrder;
    @Enumerated(EnumType.STRING) @Column(name = "weight_mode", nullable = false, length = 20) private WeightMode weightMode;
    @Column(name = "fixed_weight", precision = 8, scale = 2) private BigDecimal fixedWeight;
}
