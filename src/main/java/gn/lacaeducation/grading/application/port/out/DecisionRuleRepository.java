package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.DecisionRule;

import java.util.List;
import java.util.Optional;

public interface DecisionRuleRepository {
    DecisionRule save(DecisionRule rule);
    Optional<DecisionRule> findById(Long id);
    List<DecisionRule> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
