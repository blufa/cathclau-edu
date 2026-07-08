package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.domain.WorkflowStage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_workflows")
@Getter @Setter @NoArgsConstructor
@org.hibernate.annotations.Filter(name = "tenantFilter")
class ValidationWorkflowJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "class_id", nullable = false) private Long classId;
    @Column(name = "period_id", nullable = false) private Long periodId;
    @Enumerated(EnumType.STRING) @Column(name = "current_stage", nullable = false, length = 30) private WorkflowStage currentStage;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
}
