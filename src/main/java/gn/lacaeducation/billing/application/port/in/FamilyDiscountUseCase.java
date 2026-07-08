package gn.lacaeducation.billing.application.port.in;

import gn.lacaeducation.billing.domain.FamilyDiscountRule;
import gn.lacaeducation.billing.domain.FeeType;

import java.math.BigDecimal;
import java.util.List;

public interface FamilyDiscountUseCase {
    record RuleCommand(String label, Integer minChildren, Integer maxChildren, BigDecimal percentage,
                       FeeType feeType, Boolean appliesToYoungest, Boolean active) {}

    /** Réduction familiale applicable à un élève pour un frais donné. */
    record ApplicableDiscount(BigDecimal percentage, String ruleLabel) {}

    FamilyDiscountRule create(Long tenantId, RuleCommand command);
    FamilyDiscountRule update(Long id, RuleCommand command);
    List<FamilyDiscountRule> findByTenant(Long tenantId);
    void delete(Long id);

    /** Calcule la réduction familiale applicable à {@code studentId} pour {@code feeType} (0 % si aucune). */
    ApplicableDiscount computeApplicable(Long tenantId, Long studentId, FeeType feeType);
}
