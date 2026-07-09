package gn.lacaeducation.assignments.application.port.out;

import gn.lacaeducation.assignments.domain.Assignment;
import gn.lacaeducation.assignments.domain.AssignmentStatus;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository {
    Assignment save(Assignment a);
    Optional<Assignment> findById(Long id);
    List<Assignment> findByTenantId(Long tenantId);
    List<Assignment> findByTenantIdAndClassId(Long tenantId, Long classId);
    List<Assignment> findByTenantIdAndStatus(Long tenantId, AssignmentStatus status);
    void deleteById(Long id);
}
