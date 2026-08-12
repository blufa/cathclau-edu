package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.GradingFormulaRepository;
import gn.lacaeducation.grading.domain.GradingFormula;
import gn.lacaeducation.grading.domain.GradingFormulaStep;
import gn.lacaeducation.grading.domain.GradingFormulaStepInput;
import gn.lacaeducation.grading.domain.StepKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Assemble/désassemble l'arbre {@code GradingFormula → steps → inputs} à partir de 3 tables plates,
 * sans relation JPA (même convention que {@code report_cards}/{@code report_card_lines}) : chaque
 * enregistrement est chargé via son parent (formula_id, step_id), déjà tenant-scopé en amont.
 */
@Component
@RequiredArgsConstructor
class GradingFormulaPersistenceAdapter implements GradingFormulaRepository {

    private final GradingFormulaJpaRepository formulaJpa;
    private final GradingFormulaStepJpaRepository stepJpa;
    private final GradingFormulaStepInputJpaRepository inputJpa;

    @Override
    public GradingFormula save(GradingFormula formula) {
        GradingFormulaJpaEntity entity = formula.getId() != null
                ? formulaJpa.findById(formula.getId()).orElseGet(GradingFormulaJpaEntity::new)
                : new GradingFormulaJpaEntity();
        entity.setTenantId(formula.getTenantId());
        entity.setLabel(formula.getLabel());
        entity.setPeriodType(formula.getPeriodType());
        entity.setActive(formula.isActive());
        GradingFormulaJpaEntity saved = formulaJpa.save(entity);

        replaceSteps(saved.getId(), formula.getSteps());

        return findById(saved.getId()).orElseThrow();
    }

    private void replaceSteps(Long formulaId, List<GradingFormulaStep> steps) {
        List<GradingFormulaStepJpaEntity> existing = stepJpa.findByFormulaIdOrderByStepOrder(formulaId);
        if (!existing.isEmpty()) {
            inputJpa.deleteByStepIdIn(existing.stream().map(GradingFormulaStepJpaEntity::getId).toList());
            stepJpa.deleteByFormulaId(formulaId);
        }
        if (steps == null) return;
        for (GradingFormulaStep step : steps) {
            GradingFormulaStepJpaEntity stepEntity = new GradingFormulaStepJpaEntity();
            stepEntity.setFormulaId(formulaId);
            stepEntity.setStepOrder(step.getStepOrder());
            stepEntity.setLabel(step.getLabel());
            stepEntity.setStepKind(step.getStepKind());
            stepEntity.setMonthIndex(step.getMonthIndex());
            stepEntity.setAssessmentTypeId(step.getAssessmentTypeId());
            stepEntity.setFinalStep(step.isFinalStep());
            stepEntity.setRole(step.getRole() != null ? step.getRole() : gn.lacaeducation.grading.domain.StepRole.NONE);
            GradingFormulaStepJpaEntity savedStep = stepJpa.save(stepEntity);

            if (step.getInputs() != null) {
                for (GradingFormulaStepInput input : step.getInputs()) {
                    GradingFormulaStepInputJpaEntity inputEntity = new GradingFormulaStepInputJpaEntity();
                    inputEntity.setStepId(savedStep.getId());
                    inputEntity.setRefStepOrder(input.getRefStepOrder());
                    inputEntity.setWeightMode(input.getWeightMode());
                    inputEntity.setFixedWeight(input.getFixedWeight());
                    inputJpa.save(inputEntity);
                }
            }
        }
    }

    @Override
    public Optional<GradingFormula> findById(Long id) {
        return formulaJpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<GradingFormula> findByTenantId(Long tenantId) {
        return formulaJpa.findByTenantId(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<GradingFormula> findActiveByTenantIdAndPeriodType(Long tenantId, String periodType) {
        return formulaJpa.findByTenantIdAndPeriodTypeAndActiveTrue(tenantId, periodType).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        List<GradingFormulaStepJpaEntity> steps = stepJpa.findByFormulaIdOrderByStepOrder(id);
        inputJpa.deleteByStepIdIn(steps.stream().map(GradingFormulaStepJpaEntity::getId).toList());
        stepJpa.deleteByFormulaId(id);
        formulaJpa.deleteById(id);
    }

    private GradingFormula toDomain(GradingFormulaJpaEntity e) {
        List<GradingFormulaStep> steps = stepJpa.findByFormulaIdOrderByStepOrder(e.getId()).stream()
                .map(this::toDomain)
                .toList();
        return GradingFormula.builder()
                .id(e.getId()).tenantId(e.getTenantId()).label(e.getLabel())
                .periodType(e.getPeriodType()).active(e.isActive())
                .steps(steps)
                .build();
    }

    private GradingFormulaStep toDomain(GradingFormulaStepJpaEntity e) {
        List<GradingFormulaStepInput> inputs = e.getStepKind() == StepKind.COMBINE
                ? inputJpa.findByStepId(e.getId()).stream().map(this::toDomain).toList()
                : List.of();
        return GradingFormulaStep.builder()
                .id(e.getId()).formulaId(e.getFormulaId()).stepOrder(e.getStepOrder()).label(e.getLabel())
                .stepKind(e.getStepKind()).monthIndex(e.getMonthIndex()).assessmentTypeId(e.getAssessmentTypeId())
                .finalStep(e.isFinalStep()).inputs(inputs).role(e.getRole())
                .build();
    }

    private GradingFormulaStepInput toDomain(GradingFormulaStepInputJpaEntity e) {
        return GradingFormulaStepInput.builder()
                .id(e.getId()).refStepOrder(e.getRefStepOrder())
                .weightMode(e.getWeightMode()).fixedWeight(e.getFixedWeight())
                .build();
    }
}
