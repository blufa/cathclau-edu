package gn.lacaeducation.grading.domain;

import lombok.*;

import java.time.LocalDateTime;

/** Étape franchie d'un workflow de validation (§10) : horodatée et signée par son acteur. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ValidationStep {
    private Long id;
    private Long tenantId;
    private Long workflowId;
    private WorkflowStage stage;
    /** ADVANCE (validation) ou REJECT (renvoi à une étape antérieure). */
    private String action;
    private Long actorId;
    private String comment;
    private LocalDateTime actedAt;
}
