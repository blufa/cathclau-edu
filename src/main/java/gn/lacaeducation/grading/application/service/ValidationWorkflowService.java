package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.port.in.ValidationWorkflowUseCase;
import gn.lacaeducation.grading.application.port.out.ValidationWorkflowRepository;
import gn.lacaeducation.grading.domain.ValidationStep;
import gn.lacaeducation.grading.domain.ValidationWorkflow;
import gn.lacaeducation.grading.domain.WorkflowStage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ValidationWorkflowService implements ValidationWorkflowUseCase {

    private final ValidationWorkflowRepository repo;

    @Override
    public ValidationWorkflow getOrCreate(Long tenantId, Long classId, Long periodId) {
        return repo.findByClassIdAndPeriodId(tenantId, classId, periodId)
                .orElseGet(() -> repo.save(ValidationWorkflow.builder()
                        .tenantId(tenantId).classId(classId).periodId(periodId)
                        .currentStage(WorkflowStage.DRAFT).createdAt(LocalDateTime.now())
                        .build()));
    }

    @Override
    public ValidationWorkflow advance(Long workflowId, Long actorId, Collection<String> actorRoles, String comment) {
        ValidationWorkflow w = load(workflowId);
        WorkflowStage next = w.getCurrentStage().next()
                .orElseThrow(() -> new BusinessRuleException("Le workflow est déjà à l'étape finale (verrouillé)."));
        requireRole(actorRoles, next);
        w.setCurrentStage(next);
        w.setUpdatedAt(LocalDateTime.now());
        ValidationWorkflow saved = repo.save(w);
        recordStep(saved, next, "ADVANCE", actorId, comment);
        return saved;
    }

    @Override
    public ValidationWorkflow reject(Long workflowId, WorkflowStage targetStage, Long actorId,
                                     Collection<String> actorRoles, String comment) {
        ValidationWorkflow w = load(workflowId);
        if (targetStage == null || targetStage.compareTo(w.getCurrentStage()) >= 0) {
            throw new BusinessRuleException("Le rejet doit renvoyer à une étape antérieure à l'étape courante.");
        }
        // Le rejet est réservé aux niveaux d'encadrement (ceux qui valident l'étape courante).
        requireRole(actorRoles, w.getCurrentStage());
        w.setCurrentStage(targetStage);
        w.setUpdatedAt(LocalDateTime.now());
        ValidationWorkflow saved = repo.save(w);
        recordStep(saved, targetStage, "REJECT", actorId, comment);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationStep> history(Long workflowId) {
        return repo.findStepsByWorkflowId(workflowId);
    }

    /** Le SUPER_ADMIN passe partout ; sinon l'acteur doit avoir un rôle autorisé pour l'étape. */
    private void requireRole(Collection<String> actorRoles, WorkflowStage stage) {
        if (actorRoles == null) throw new AccessDeniedException("Rôle requis pour cette étape.");
        if (actorRoles.contains("SUPER_ADMIN")) return;
        boolean allowed = stage.requiredRoles().stream().anyMatch(actorRoles::contains);
        if (!allowed) {
            throw new AccessDeniedException("Votre rôle ne permet pas de franchir l'étape " + stage + ".");
        }
    }

    private void recordStep(ValidationWorkflow w, WorkflowStage stage, String action, Long actorId, String comment) {
        repo.saveStep(ValidationStep.builder()
                .tenantId(w.getTenantId()).workflowId(w.getId()).stage(stage).action(action)
                .actorId(actorId).comment(comment).actedAt(LocalDateTime.now())
                .build());
    }

    private ValidationWorkflow load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("ValidationWorkflow", id));
    }
}
