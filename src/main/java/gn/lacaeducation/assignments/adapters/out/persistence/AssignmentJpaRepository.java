package gn.lacaeducation.assignments.adapters.out.persistence;

import gn.lacaeducation.assignments.domain.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AssignmentJpaRepository extends JpaRepository<AssignmentJpaEntity, Long> {
    List<AssignmentJpaEntity> findByTenantId(Long tenantId);
    List<AssignmentJpaEntity> findByTenantIdAndClassId(Long tenantId, Long classId);
    List<AssignmentJpaEntity> findByTenantIdAndStatus(Long tenantId, AssignmentStatus status);
}
