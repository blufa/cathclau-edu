package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.GradingFormulaUseCase;
import gn.lacaeducation.grading.application.port.out.GradingFormulaRepository;
import gn.lacaeducation.grading.domain.GradingFormula;
import gn.lacaeducation.grading.domain.GradingFormulaStep;
import gn.lacaeducation.grading.domain.GradingFormulaStepInput;
import gn.lacaeducation.grading.domain.StepKind;
import gn.lacaeducation.grading.domain.WeightMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * CRUD des formules de calcul de moyenne (§ Refonte du moteur de calcul des notes). Garantit qu'une
 * formule est exécutable par {@code GradingFormulaEngine} : une seule étape finale, et chaque entrée
 * d'une étape {@code COMBINE} référence une étape antérieure existante (l'ordre d'exécution est
 * l'ordre croissant de {@code stepOrder}).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GradingFormulaService implements GradingFormulaUseCase {

    private final GradingFormulaRepository repo;

    @Override
    public GradingFormula create(Long tenantId, FormulaCommand c) {
        validate(c);
        boolean active = c.active() == null || c.active();
        if (active) {
            deactivateOthers(tenantId, c.periodType(), null);
        }
        GradingFormula formula = GradingFormula.builder()
                .tenantId(tenantId).label(c.label()).periodType(c.periodType()).active(active)
                .steps(toSteps(c.steps()))
                .build();
        return repo.save(formula);
    }

    @Override
    public GradingFormula update(Long id, FormulaCommand c) {
        validate(c);
        GradingFormula existing = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("GradingFormula", id));
        boolean active = c.active() == null || c.active();
        if (active) {
            deactivateOthers(existing.getTenantId(), c.periodType(), id);
        }
        existing.setLabel(c.label());
        existing.setPeriodType(c.periodType());
        existing.setActive(active);
        existing.setSteps(toSteps(c.steps()));
        return repo.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradingFormula> findByTenant(Long tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /** Désactive l'ancienne formule active du même (tenant, type de période) pour n'en garder qu'une seule. */
    private void deactivateOthers(Long tenantId, String periodType, Long excludeId) {
        repo.findActiveByTenantIdAndPeriodType(tenantId, periodType)
                .filter(f -> excludeId == null || !f.getId().equals(excludeId))
                .ifPresent(f -> {
                    f.setActive(false);
                    repo.save(f);
                });
    }

    private List<GradingFormulaStep> toSteps(List<StepCommand> commands) {
        if (commands == null) return List.of();
        return commands.stream()
                .map(s -> GradingFormulaStep.builder()
                        .stepOrder(s.stepOrder())
                        .label(s.label())
                        .stepKind(s.stepKind())
                        .monthIndex(s.monthIndex())
                        .assessmentTypeId(s.assessmentTypeId())
                        .finalStep(s.finalStep() != null && s.finalStep())
                        .inputs(toInputs(s.inputs()))
                        .build())
                .toList();
    }

    private List<GradingFormulaStepInput> toInputs(List<StepInputCommand> commands) {
        if (commands == null) return List.of();
        return commands.stream()
                .map(i -> GradingFormulaStepInput.builder()
                        .refStepOrder(i.refStepOrder())
                        .weightMode(i.weightMode())
                        .fixedWeight(i.fixedWeight())
                        .build())
                .toList();
    }

    private void validate(FormulaCommand c) {
        if (c.label() == null || c.label().isBlank()) throw new BusinessRuleException("Le nom de la formule est requis.");
        if (c.periodType() == null || c.periodType().isBlank()) throw new BusinessRuleException("Le type de période est requis.");
        if (c.steps() == null || c.steps().isEmpty()) throw new BusinessRuleException("La formule doit comporter au moins une étape.");

        Set<Integer> orders = new java.util.HashSet<>();
        long finalCount = 0;
        for (StepCommand s : c.steps()) {
            if (s.stepOrder() == null) throw new BusinessRuleException("Chaque étape doit avoir un ordre.");
            if (!orders.add(s.stepOrder())) {
                throw new BusinessRuleException("L'ordre des étapes doit être unique (doublon : " + s.stepOrder() + ").");
            }
            if (s.stepKind() == null) throw new BusinessRuleException("Chaque étape doit préciser son type (GROUP ou COMBINE).");
            if (Boolean.TRUE.equals(s.finalStep())) finalCount++;
            if (s.stepKind() == StepKind.COMBINE) {
                if (s.inputs() == null || s.inputs().isEmpty()) {
                    throw new BusinessRuleException("Une étape de combinaison doit référencer au moins une étape précédente.");
                }
                for (StepInputCommand input : s.inputs()) {
                    if (input.refStepOrder() == null || input.refStepOrder() >= s.stepOrder()) {
                        throw new BusinessRuleException(
                                "Une étape de combinaison ne peut référencer qu'une étape d'ordre antérieur.");
                    }
                    if (input.weightMode() == WeightMode.FIXED && input.fixedWeight() == null) {
                        throw new BusinessRuleException("Un poids fixe doit préciser sa valeur.");
                    }
                }
            }
        }
        if (finalCount != 1) {
            throw new BusinessRuleException("La formule doit comporter exactement une étape finale.");
        }
    }
}
