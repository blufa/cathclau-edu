package gn.lacaeducation.grading.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ValidationWorkflowJpaRepository extends JpaRepository<ValidationWorkflowJpaEntity, Long> {
    Optional<ValidationWorkflowJpaEntity> findByTenantIdAndClassIdAndPeriodId(Long tenantId, Long classId, Long periodId);
}
