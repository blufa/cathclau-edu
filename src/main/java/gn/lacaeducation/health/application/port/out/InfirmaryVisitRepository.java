package gn.lacaeducation.health.application.port.out;

import gn.lacaeducation.health.domain.InfirmaryVisit;

import java.util.List;
import java.util.Optional;

public interface InfirmaryVisitRepository {
    InfirmaryVisit save(InfirmaryVisit visit);
    Optional<InfirmaryVisit> findById(Long id);
    List<InfirmaryVisit> findByTenantId(Long tenantId);
    List<InfirmaryVisit> findByStudentId(Long studentId);
    void deleteById(Long id);
}
