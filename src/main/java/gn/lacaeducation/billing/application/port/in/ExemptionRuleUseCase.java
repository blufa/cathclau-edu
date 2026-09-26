package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.ExemptionRule;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;

import java.math.BigDecimal;
import java.util.List;

/** Catalogue des types d'exonération pré-configurés — voir {@link ExemptionRule}. */
public interface ExemptionRuleUseCase {
    /** {@code feeTypes} : un seul type, plusieurs, ou vide/null = tous les frais. {@code staffBenefit} :
     * avantage personnel (enfant de membre du personnel) — voir {@link ExemptionRule#isStaffBenefit()}. */
    record RuleCommand(String label, ExemptionType type, BigDecimal percentage, BigDecimal amount,
                       List<FeeType> feeTypes, Boolean staffBenefit, Boolean active) {}

    ExemptionRule create(Long tenantId, RuleCommand command);
    ExemptionRule update(Long id, RuleCommand command);
    List<ExemptionRule> findByTenant(Long tenantId);
    void delete(Long id);
}
