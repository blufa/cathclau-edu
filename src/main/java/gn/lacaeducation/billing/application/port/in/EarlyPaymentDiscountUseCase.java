package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.EarlyPaymentDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;

import java.math.BigDecimal;
import java.util.List;

public interface EarlyPaymentDiscountUseCase {
    record RuleCommand(String label, BigDecimal percentage, FeeType feeType, Boolean active) {}

    /** Remise pour paiement annuel complet applicable à un type de frais donné. */
    record ApplicableDiscount(BigDecimal percentage, String ruleLabel) {}

    EarlyPaymentDiscountRule create(Long tenantId, RuleCommand command);
    EarlyPaymentDiscountRule update(Long id, RuleCommand command);
    List<EarlyPaymentDiscountRule> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la remise applicable pour {@code feeType} (0 % si aucune règle active). */
    ApplicableDiscount computeApplicable(Long tenantId, FeeType feeType);
}
