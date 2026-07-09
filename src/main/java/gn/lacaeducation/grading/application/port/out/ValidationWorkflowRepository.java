package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.ValidationStep;
import gn.lacaeducation.grading.domain.ValidationWorkflow;

import java.util.List;
import java.util.Optional;

public interface ValidationWorkflowRepository {
    ValidationWorkflow save(ValidationWorkflow workflow);
    Optional<ValidationWorkflow> findById(Long id);
    Optional<ValidationWorkflow> findByClassIdAndPeriodId(Long tenantId, Long classId, Long periodId);
    ValidationStep saveStep(ValidationStep step);
    List<ValidationStep> findStepsByWorkflowId(Long workflowId);
}
