package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.SalaryStructure;

import java.util.List;
import java.util.Optional;

public interface SalaryStructureRepository {
    SalaryStructure save(SalaryStructure s);
    Optional<SalaryStructure> findById(Long id);
    List<SalaryStructure> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
