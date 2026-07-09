package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Applique les notes éliminatoires (§7) : si une moyenne de matière est strictement inférieure au
 * seuil d'une règle active, la décision devient celle imposée par la règle. Fonction pure.
 */
public final class EliminatoryRuleEvaluator {

    private EliminatoryRuleEvaluator() {}

    /**
     * @param baseDecision    décision calculée à partir du barème (ex. ADMIS/REDOUBLE)
     * @param subjectAverages moyenne par matière (subjectId → moyenne)
     * @param rules           règles éliminatoires du tenant
     * @return la décision imposée par la première règle déclenchée, sinon {@code baseDecision}
     */
    public static String resolveDecision(String baseDecision, Map<Long, BigDecimal> subjectAverages,
                                         List<EliminatoryRule> rules) {
        if (rules == null || subjectAverages == null) return baseDecision;
        for (EliminatoryRule rule : rules) {
            if (!rule.isActive() || rule.getSubjectId() == null || rule.getThreshold() == null) continue;
            BigDecimal average = subjectAverages.get(rule.getSubjectId());
            if (average != null && average.compareTo(rule.getThreshold()) < 0) {
                return rule.getDecisionLabel() != null ? rule.getDecisionLabel() : baseDecision;
            }
        }
        return baseDecision;
    }
}
