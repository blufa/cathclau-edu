package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.EliminatoryRule;

import java.math.BigDecimal;
import java.util.List;

public interface EliminatoryRuleUseCase {
    record RuleCommand(Long subjectId, BigDecimal threshold, String decisionLabel, Boolean active) {}

    EliminatoryRule create(Long tenantId, RuleCommand command);
    EliminatoryRule update(Long id, RuleCommand command);
    List<EliminatoryRule> findByTenant(Long tenantId);
    void delete(Long id);
}
