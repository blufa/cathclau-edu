package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.ValidationStep;
import gn.lacaeducation.grading.domain.ValidationWorkflow;
import gn.lacaeducation.grading.domain.WorkflowStage;

import java.util.Collection;
import java.util.List;

public interface ValidationWorkflowUseCase {

    /** Récupère (ou crée en DRAFT) le workflow d'une classe/période. */
    ValidationWorkflow getOrCreate(Long tenantId, Long classId, Long periodId);

    /** Fait avancer le workflow à l'étape suivante, signée par l'acteur (rôle autorisé requis). */
    ValidationWorkflow advance(Long workflowId, Long actorId, Collection<String> actorRoles, String comment);

    /** Renvoie le workflow à une étape antérieure (rejet), tracé et signé. */
    ValidationWorkflow reject(Long workflowId, WorkflowStage targetStage, Long actorId,
                              Collection<String> actorRoles, String comment);

    List<ValidationStep> history(Long workflowId);
}
