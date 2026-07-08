package gn.lacaeducation.grading.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionRuleEngineTest {

    private DecisionRule rule(String name, DecisionConditionType type, String threshold, Long subjectId,
                              Integer minCount, String decision, int priority) {
        return DecisionRule.builder().name(name).conditionType(type)
                .threshold(threshold == null ? null : new BigDecimal(threshold))
                .subjectId(subjectId).minCount(minCount).decision(decision).priority(priority).active(true).build();
    }

    @Test
    void firstMatchingRuleByPriority_wins() {
        // priorité 1 : Maths < 5 → REDOUBLE ; priorité 2 : moyenne ≥ 10 → ADMIS
        List<DecisionRule> rules = List.of(
                rule("Maths éliminatoire", DecisionConditionType.SUBJECT_BELOW, "5", 1L, null, "REDOUBLE", 1),
                rule("Passage", DecisionConditionType.OVERALL_AT_LEAST, "10", null, null, "ADMIS", 2));
        Map<Long, BigDecimal> subjects = Map.of(1L, new BigDecimal("4"), 2L, new BigDecimal("15"));

        String decision = DecisionRuleEngine.evaluate(new BigDecimal("12"), subjects, rules, LocalDate.now());

        assertThat(decision).isEqualTo("REDOUBLE"); // la règle prioritaire (Maths) l'emporte
    }

    @Test
    void overallAtLeast_admits() {
        List<DecisionRule> rules = List.of(
                rule("Passage", DecisionConditionType.OVERALL_AT_LEAST, "10", null, null, "ADMIS", 1));

        assertThat(DecisionRuleEngine.evaluate(new BigDecimal("11"), Map.of(), rules, LocalDate.now()))
                .isEqualTo("ADMIS");
    }

    @Test
    void subjectsBelowCount_triggersRetake() {
        // 2 matières < 7 → RATTRAPAGE
        List<DecisionRule> rules = List.of(
                rule("Rattrapage", DecisionConditionType.SUBJECTS_BELOW_COUNT, "7", null, 2, "RATTRAPAGE", 1));
        Map<Long, BigDecimal> subjects = Map.of(1L, new BigDecimal("6"), 2L, new BigDecimal("5"), 3L, new BigDecimal("14"));

        assertThat(DecisionRuleEngine.evaluate(new BigDecimal("9"), subjects, rules, LocalDate.now()))
                .isEqualTo("RATTRAPAGE");
    }

    @Test
    void inactiveOrOutOfDateRule_ignored() {
        DecisionRule expired = rule("Ancienne", DecisionConditionType.OVERALL_AT_LEAST, "10", null, null, "ADMIS", 1);
        expired.setValidTo(LocalDate.of(2025, 12, 31));

        String decision = DecisionRuleEngine.evaluate(new BigDecimal("15"), Map.of(), List.of(expired),
                LocalDate.of(2026, 7, 1));

        assertThat(decision).isNull(); // règle expirée → non appliquée
    }

    @Test
    void noRuleMatches_returnsNull() {
        List<DecisionRule> rules = List.of(
                rule("Passage", DecisionConditionType.OVERALL_AT_LEAST, "10", null, null, "ADMIS", 1));

        assertThat(DecisionRuleEngine.evaluate(new BigDecimal("8"), Map.of(), rules, LocalDate.now())).isNull();
    }
}
