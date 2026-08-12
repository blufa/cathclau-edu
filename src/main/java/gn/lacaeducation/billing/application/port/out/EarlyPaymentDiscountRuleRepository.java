package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.EarlyPaymentDiscountRule;

import java.util.List;
import java.util.Optional;

public interface EarlyPaymentDiscountRuleRepository {
    EarlyPaymentDiscountRule save(EarlyPaymentDiscountRule rule);
    Optional<EarlyPaymentDiscountRule> findById(Long id);
    List<EarlyPaymentDiscountRule> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
