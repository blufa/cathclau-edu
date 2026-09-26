package gn.lacaeducation.billing.application.port.out;

import gn.lacaeducation.billing.domain.ExemptionRule;

import java.util.List;
import java.util.Optional;

public interface ExemptionRuleRepository {
    ExemptionRule save(ExemptionRule rule);
    Optional<ExemptionRule> findById(Long id);
    List<ExemptionRule> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
