package gn.lacaeducation.payroll.adapters.out;

import gn.lacaeducation.payroll.SchoolPayrollStatsProvider;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.domain.Payslip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;

/** Masse salariale récurrente = total net du mois de paie le plus récent (§10.2). */
@Component
@RequiredArgsConstructor
class SchoolPayrollStatsProviderAdapter implements SchoolPayrollStatsProvider {

    private final PayslipRepository payslipRepository;

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
}
