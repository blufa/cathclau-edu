package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.GradingFormula;
import gn.lacaeducation.grading.domain.StepKind;
import gn.lacaeducation.grading.domain.StepRole;
import gn.lacaeducation.grading.domain.WeightMode;

import java.math.BigDecimal;
import java.util.List;

public interface GradingFormulaUseCase {

    record StepInputCommand(Integer refStepOrder, WeightMode weightMode, BigDecimal fixedWeight) {}

    record StepCommand(Integer stepOrder, String label, StepKind stepKind, Integer monthIndex,
                       Long assessmentTypeId, List<StepInputCommand> inputs, Boolean finalStep, StepRole role) {}

    record FormulaCommand(String label, String periodType, Boolean active, List<StepCommand> steps) {}

    GradingFormula create(Long tenantId, FormulaCommand command);
    GradingFormula update(Long id, FormulaCommand command);
    List<GradingFormula> findByTenant(Long tenantId);
    void delete(Long id);
}
