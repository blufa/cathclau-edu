package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ValidationStepJpaRepository extends JpaRepository<ValidationStepJpaEntity, Long> {
    List<ValidationStepJpaEntity> findByWorkflowIdOrderByActedAtAsc(Long workflowId);
}
