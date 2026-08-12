package gn.lacaeducation.payroll.adapters.out;

import gn.lacaeducation.payroll.SchoolPayrollStatsProvider;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Masse salariale récurrente = total net du mois de paie le plus récent (§10.2). */
@Component
@RequiredArgsConstructor
class SchoolPayrollStatsProviderAdapter implements SchoolPayrollStatsProvider {

    private final PayslipRepository payslipRepository;
    private final PayrollService payrollService;
    private final EmployeePublicInfoProvider employeePublicInfoProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal recurringMonthlyPayroll(Long tenantId) {
        Map<YearMonth, BigDecimal> byMonth = payslipRepository.findByTenantId(tenantId).stream()
                .filter(p -> p.getPeriodStart() != null && p.getNetSalary() != null)
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getPeriodStart()),
                        Collectors.reducing(BigDecimal.ZERO, Payslip::getNetSalary, BigDecimal::add)));
        return byMonth.entrySet().stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollMassSummary payrollMassSummary(Long tenantId, int year, int month) {
        PayrollService.TeacherPayrollMass teachers = payrollService.teacherPayrollMass(tenantId, year, month);
        BigDecimal staffPayroll = employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active)
                .map(EmployeePublicInfoProvider.EmployeeInfo::baseSalary)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int staffCount = (int) employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active).count();
        return new PayrollMassSummary(teachers.totalHourlyRateNormal(), teachers.totalHourlyRateExam(),
                teachers.monthlyPayroll(), staffPayroll, teachers.monthlyPayroll().add(staffPayroll),
                teachers.teacherCount(), staffCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherHoursProgress> teacherHoursProgress(Long tenantId, int year, int month) {
        return teacherPublicInfoProvider.listTeachers(tenantId).stream()
                .map(t -> {
                    String name = ((t.firstName() != null ? t.firstName() : "") + " "
                            + (t.lastName() != null ? t.lastName() : "")).trim();
                    PayrollService.TheoreticalMonthlyPay due = payrollService.computeTheoreticalMonthlyPay(t.id(), year, month);
                    int realized = payrollService.computeMonthlyPay(t.id(), year, month).realizedMinutes();
                    return new TeacherHoursProgress(t.id(), name, due.normalMinutes() + due.examMinutes(), realized);
                })
                .toList();
    }
}
