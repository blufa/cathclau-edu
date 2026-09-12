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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        BigDecimal staffPayroll = activeStaffPayroll(tenantId);
        int staffCount = (int) employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active).count();
        return new PayrollMassSummary(teachers.totalHourlyRateNormal(), teachers.totalHourlyRateExam(),
                teachers.monthlyPayroll(), staffPayroll, teachers.monthlyPayroll().add(staffPayroll),
                teachers.teacherCount(), staffCount);
    }

    @Override
    @Transactional(readOnly = true)
    public RealPayrollSummary realPayrollSummary(Long tenantId, int year, int month) {
        PayrollService.RealTeacherPayrollMass teachers = payrollService.realTeacherPayrollMass(tenantId, year, month);
        BigDecimal staffPayroll = activeStaffPayroll(tenantId);
        return new RealPayrollSummary(teachers.monthlyPayroll(), staffPayroll,
                teachers.monthlyPayroll().add(staffPayroll), teachers.totalLateMinutes(), teachers.totalEarlyMinutes(),
                teachers.totalRealizedMinutes());
    }

    private BigDecimal activeStaffPayroll(Long tenantId) {
        return employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active)
                .map(EmployeePublicInfoProvider.EmployeeInfo::baseSalary)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    @Override
    @Transactional(readOnly = true)
    public SchoolYearProjection projectSchoolYear(Long tenantId, int startYear, int startMonth, int monthsCount) {
        BigDecimal staffMonthly = employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active)
                .map(EmployeePublicInfoProvider.EmployeeInfo::baseSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MonthProjection> months = new ArrayList<>();
        BigDecimal totalTeachers = BigDecimal.ZERO;
        YearMonth cursor = YearMonth.of(startYear, startMonth);
        for (int i = 0; i < monthsCount; i++) {
            BigDecimal teacherMonthly = payrollService.teacherPayrollMass(tenantId, cursor.getYear(), cursor.getMonthValue()).monthlyPayroll();
            totalTeachers = totalTeachers.add(teacherMonthly);
            months.add(new MonthProjection(cursor.getYear(), cursor.getMonthValue(), teacherMonthly, staffMonthly,
                    teacherMonthly.add(staffMonthly)));
            cursor = cursor.plusMonths(1);
        }
        BigDecimal totalStaff = staffMonthly.multiply(BigDecimal.valueOf(monthsCount));
        return new SchoolYearProjection(months, totalTeachers, totalStaff, totalTeachers.add(totalStaff));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSalary> teacherSalaries(Long tenantId, int year, int month, int monthsCount) {
        Map<Long, String> nameById = teacherPublicInfoProvider.listTeachers(tenantId).stream()
                .collect(Collectors.toMap(
                        t -> t.id(),
                        t -> ((t.firstName() != null ? t.firstName() : "") + " " + (t.lastName() != null ? t.lastName() : "")).trim()));
        return payrollService.teacherSalaryBreakdown(tenantId, year, month, monthsCount).stream()
                .map(b -> new TeacherSalary(b.teacherId(), nameById.getOrDefault(b.teacherId(), "#" + b.teacherId()),
                        b.monthlyPay(), b.annualProjection()))
                .toList();
    }
}
