package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryAdvance;

import java.util.List;
import java.util.Optional;

public interface SalaryAdvanceRepository {
    SalaryAdvance save(SalaryAdvance advance);
    Optional<SalaryAdvance> findById(Long id);
    List<SalaryAdvance> findByTenantId(Long tenantId);
    List<SalaryAdvance> findByTenantIdAndTeacherIdAndEmployeeType(Long tenantId, Long teacherId, EmployeeType employeeType);
}
