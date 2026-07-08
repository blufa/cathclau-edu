package gn.lacaeducation.grading.application.port.in;

import gn.lacaeducation.grading.domain.DecisionConditionType;
import gn.lacaeducation.grading.domain.DecisionRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DecisionRuleUseCase {
    record RuleCommand(String name, DecisionConditionType conditionType, BigDecimal threshold,
                       Long subjectId, Integer minCount, String decision, Integer priority,
                       Boolean active, LocalDate validFrom, LocalDate validTo) {}

    DecisionRule create(Long tenantId, RuleCommand command);
    DecisionRule update(Long id, RuleCommand command);
    List<DecisionRule> findByTenant(Long tenantId);
    void delete(Long id);
}
