package gn.lacaeducation.grading.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EliminatoryRuleEvaluatorTest {

    // Règle : Mathématiques (matière 1) < 5 → REDOUBLE.
    private final List<EliminatoryRule> rules = List.of(
            EliminatoryRule.builder().subjectId(1L).threshold(new BigDecimal("5")).decisionLabel("REDOUBLE").active(true).build());

    @Test
    void belowThreshold_imposesRuleDecision() {
        Map<Long, BigDecimal> averages = Map.of(1L, new BigDecimal("4.5"), 2L, new BigDecimal("15"));

        assertThat(EliminatoryRuleEvaluator.resolveDecision("ADMIS", averages, rules)).isEqualTo("REDOUBLE");
    }

    @Test
    void atOrAboveThreshold_keepsBaseDecision() {
        Map<Long, BigDecimal> averages = Map.of(1L, new BigDecimal("5"), 2L, new BigDecimal("15"));

        assertThat(EliminatoryRuleEvaluator.resolveDecision("ADMIS", averages, rules)).isEqualTo("ADMIS");
    }

    @Test
    void inactiveRule_isIgnored() {
        List<EliminatoryRule> inactive = List.of(
                EliminatoryRule.builder().subjectId(1L).threshold(new BigDecimal("5")).decisionLabel("REDOUBLE").active(false).build());
        Map<Long, BigDecimal> averages = Map.of(1L, new BigDecimal("2"));

        assertThat(EliminatoryRuleEvaluator.resolveDecision("ADMIS", averages, inactive)).isEqualTo("ADMIS");
    }

    @Test
    void noRules_keepsBaseDecision() {
        assertThat(EliminatoryRuleEvaluator.resolveDecision("ADMIS", Map.of(1L, new BigDecimal("2")), List.of()))
                .isEqualTo("ADMIS");
    }
}
