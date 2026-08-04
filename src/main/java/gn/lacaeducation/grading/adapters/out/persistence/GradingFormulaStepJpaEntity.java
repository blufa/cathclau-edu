package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.StepKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pas de tenant_id/filtre tenant ici : toujours chargée via {@code formula_id}, déjà tenant-scopé
 * via {@link GradingFormulaJpaEntity} (même convention que {@code report_card_lines}). */
@Entity
@Table(name = "grading_formula_step")
@Getter @Setter @NoArgsConstructor
class GradingFormulaStepJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "formula_id", nullable = false) private Long formulaId;
    @Column(name = "step_order", nullable = false) private int stepOrder;
    private String label;
    @Enumerated(EnumType.STRING) @Column(name = "step_kind", nullable = false, length = 20) private StepKind stepKind;
    @Column(name = "month_index") private Integer monthIndex;
    @Column(name = "assessment_type_id") private Long assessmentTypeId;
    @Column(name = "is_final", nullable = false) private boolean finalStep;
}
