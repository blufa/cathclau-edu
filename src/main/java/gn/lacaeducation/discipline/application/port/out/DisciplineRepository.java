package gn.lacaeducation.discipline.application.port.out;

import gn.lacaeducation.discipline.domain.DisciplineIncident;
import gn.lacaeducation.discipline.domain.DisciplineStatus;

import java.util.List;
import java.util.Optional;

public interface DisciplineRepository {
    DisciplineIncident save(DisciplineIncident i);
    Optional<DisciplineIncident> findById(Long id);
    List<DisciplineIncident> findByTenantId(Long tenantId);
    List<DisciplineIncident> findByTenantIdAndStudentId(Long tenantId, Long studentId);
    List<DisciplineIncident> findByTenantIdAndStatus(Long tenantId, DisciplineStatus status);
    void deleteById(Long id);
}
