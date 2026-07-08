package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipStatus;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository {
    Payslip save(Payslip p);
    Optional<Payslip> findById(Long id);
    List<Payslip> findByTenantId(Long tenantId);
    List<Payslip> findByTenantIdAndTeacherId(Long tenantId, Long teacherId);
    List<Payslip> findByTenantIdAndStatus(Long tenantId, PayslipStatus status);
    void deleteById(Long id);
}
