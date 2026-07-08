package gn.lacaeducation.payroll.application.port.in;

import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;

import java.math.BigDecimal;
import java.util.List;

public interface SalaryFeeDeductionUseCase {
    record RuleCommand(String label, BigDecimal amountPerChild, Integer maxChildren, BigDecimal cap, Boolean active) {}

    /** Retenue de frais scolaires calculée pour un employé. */
    record FeeDeduction(BigDecimal amount, String label, int childrenCount) {}

    SalaryFeeDeductionRule create(Long tenantId, RuleCommand command);
    SalaryFeeDeductionRule update(Long id, RuleCommand command);
    List<SalaryFeeDeductionRule> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la retenue de frais scolaires pour un employé (0 s'il n'a pas d'enfant ou pas de règle). */
    FeeDeduction computeFeeDeduction(Long tenantId, Long teacherId);
}
