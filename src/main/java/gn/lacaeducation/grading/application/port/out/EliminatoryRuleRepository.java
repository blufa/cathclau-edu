package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.EliminatoryRule;

import java.util.List;
import java.util.Optional;

public interface EliminatoryRuleRepository {
    EliminatoryRule save(EliminatoryRule rule);
    Optional<EliminatoryRule> findById(Long id);
    List<EliminatoryRule> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
