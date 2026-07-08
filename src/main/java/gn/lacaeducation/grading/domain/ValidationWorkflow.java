package gn.lacaeducation.grading.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Workflow de validation des bulletins d'une classe pour une période (§10). Un seul workflow par
 * (classe, période) ; son étape courante progresse séquentiellement, chaque transition étant tracée
 * dans un {@link ValidationStep}.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationWorkflow {
    private Long id;
    private Long tenantId;
    private Long classId;
    private Long periodId;
    private WorkflowStage currentStage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
