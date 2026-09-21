package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.EmployeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SalaryAdvanceJpaRepository extends JpaRepository<SalaryAdvanceJpaEntity, Long> {
    List<SalaryAdvanceJpaEntity> findByTenantId(Long tenantId);
    List<SalaryAdvanceJpaEntity> findByTenantIdAndTeacherIdAndEmployeeType(
            Long tenantId, Long teacherId, EmployeeType employeeType);
}
