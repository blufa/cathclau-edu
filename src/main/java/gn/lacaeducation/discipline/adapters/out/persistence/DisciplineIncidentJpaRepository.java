package gn.lacaeducation.discipline.adapters.out.persistence;

import gn.lacaeducation.discipline.domain.DisciplineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface DisciplineIncidentJpaRepository extends JpaRepository<DisciplineIncidentJpaEntity, Long> {
    List<DisciplineIncidentJpaEntity> findByTenantId(Long tenantId);
    List<DisciplineIncidentJpaEntity> findByTenantIdAndStudentId(Long tenantId, Long studentId);
    List<DisciplineIncidentJpaEntity> findByTenantIdAndStatus(Long tenantId, DisciplineStatus status);
}
