package gn.lacaeducation.payroll.adapters.out.persistence;

import gn.lacaeducation.payroll.domain.PayslipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PayslipJpaRepository extends JpaRepository<PayslipJpaEntity, Long> {
    List<PayslipJpaEntity> findByTenantId(Long tenantId);
    List<PayslipJpaEntity> findByTenantIdAndTeacherId(Long tenantId, Long teacherId);
    List<PayslipJpaEntity> findByTenantIdAndStatus(Long tenantId, PayslipStatus status);
    boolean existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(
            Long tenantId, Long teacherId, String periodLabel, gn.lacaeducation.payroll.domain.EmployeeType employeeType);
}
