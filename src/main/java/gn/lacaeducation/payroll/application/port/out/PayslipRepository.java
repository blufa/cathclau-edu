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
    /** Pour empêcher de générer deux fois la même fiche (même salarié, même période) — un double-clic
     * sur « Générer en lot »/« Générer une fiche » ne doit pas créer un doublon silencieux.
     * {@code employeeType} est inclus car {@code teacherId} référence deux référentiels distincts
     * (Teacher ou Employee selon le type) dont les identifiants peuvent coïncider. */
    boolean existsByTenantIdAndTeacherIdAndPeriodLabelAndEmployeeType(
            Long tenantId, Long teacherId, String periodLabel, gn.lacaeducation.payroll.domain.EmployeeType employeeType);
    void deleteById(Long id);
}
