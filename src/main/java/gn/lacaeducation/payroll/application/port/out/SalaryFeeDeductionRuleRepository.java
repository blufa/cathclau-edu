package gn.lacaeducation.payroll.application.port.out;

import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;

import java.util.List;
import java.util.Optional;

public interface SalaryFeeDeductionRuleRepository {
    SalaryFeeDeductionRule save(SalaryFeeDeductionRule rule);
    Optional<SalaryFeeDeductionRule> findById(Long id);
    List<SalaryFeeDeductionRule> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
