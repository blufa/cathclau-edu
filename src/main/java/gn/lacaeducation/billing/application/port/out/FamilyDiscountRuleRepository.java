package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.FamilyDiscountRule;

import java.util.List;
import java.util.Optional;

public interface FamilyDiscountRuleRepository {
    FamilyDiscountRule save(FamilyDiscountRule rule);
    Optional<FamilyDiscountRule> findById(Long id);
    List<FamilyDiscountRule> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
