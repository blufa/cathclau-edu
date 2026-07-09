package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.Exemption;

import java.util.List;
import java.util.Optional;

public interface ExemptionRepository {
    Exemption save(Exemption exemption);
    Optional<Exemption> findById(Long id);
    List<Exemption> findByTenantId(Long tenantId);
    List<Exemption> findByStudentId(Long studentId);
}
