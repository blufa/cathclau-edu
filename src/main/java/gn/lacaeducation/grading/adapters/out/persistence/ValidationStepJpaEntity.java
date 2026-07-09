package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.WorkflowStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_steps")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class ValidationStepJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "workflow_id", nullable = false) private Long workflowId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private WorkflowStage stage;
    @Column(nullable = false, length = 20) private String action;
    @Column(name = "actor_id") private Long actorId;
    @Column(length = 500) private String comment;
    @Column(name = "acted_at", nullable = false) private LocalDateTime actedAt;
}
