package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.ValidationWorkflowRepository;
import gn.lacaeducation.grading.domain.ValidationStep;
import gn.lacaeducation.grading.domain.ValidationWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ValidationWorkflowPersistenceAdapter implements ValidationWorkflowRepository {

    private final ValidationWorkflowJpaRepository workflowJpa;
    private final ValidationStepJpaRepository stepJpa;

    @Override
    public ValidationWorkflow save(ValidationWorkflow w) {
        ValidationWorkflowJpaEntity e = w.getId() != null
                ? workflowJpa.findById(w.getId()).orElseGet(ValidationWorkflowJpaEntity::new)
                : new ValidationWorkflowJpaEntity();
        e.setTenantId(w.getTenantId()); e.setClassId(w.getClassId()); e.setPeriodId(w.getPeriodId());
        e.setCurrentStage(w.getCurrentStage()); e.setCreatedAt(w.getCreatedAt()); e.setUpdatedAt(w.getUpdatedAt());
        return toDomain(workflowJpa.save(e));
    }

    @Override
    public Optional<ValidationWorkflow> findById(Long id) {
        return workflowJpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ValidationWorkflow> findByClassIdAndPeriodId(Long tenantId, Long classId, Long periodId) {
        return workflowJpa.findByTenantIdAndClassIdAndPeriodId(tenantId, classId, periodId).map(this::toDomain);
    }

    @Override
    public ValidationStep saveStep(ValidationStep s) {
        ValidationStepJpaEntity e = new ValidationStepJpaEntity();
        e.setTenantId(s.getTenantId()); e.setWorkflowId(s.getWorkflowId()); e.setStage(s.getStage());
        e.setAction(s.getAction()); e.setActorId(s.getActorId()); e.setComment(s.getComment());
        e.setActedAt(s.getActedAt());
        return toDomain(stepJpa.save(e));
    }

    @Override
    public List<ValidationStep> findStepsByWorkflowId(Long workflowId) {
        return stepJpa.findByWorkflowIdOrderByActedAtAsc(workflowId).stream().map(this::toDomain).toList();
    }

    private ValidationWorkflow toDomain(ValidationWorkflowJpaEntity e) {
        return ValidationWorkflow.builder()
                .id(e.getId()).tenantId(e.getTenantId()).classId(e.getClassId()).periodId(e.getPeriodId())
                .currentStage(e.getCurrentStage()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }

    private ValidationStep toDomain(ValidationStepJpaEntity e) {
        return ValidationStep.builder()
                .id(e.getId()).tenantId(e.getTenantId()).workflowId(e.getWorkflowId()).stage(e.getStage())
                .action(e.getAction()).actorId(e.getActorId()).comment(e.getComment()).actedAt(e.getActedAt())
                .build();
    }
}
