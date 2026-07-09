package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Moteur de règles de décision (§17). Évalue les règles actives et valides à une date, par ordre de
 * priorité, et renvoie la décision de la première règle satisfaite. Fonction pure.
 */
public final class DecisionRuleEngine {

    private DecisionRuleEngine() {}

    /**
     * @param overallAverage  moyenne générale de l'élève (peut être nulle)
     * @param subjectAverages moyenne par matière (subjectId → moyenne)
     * @param rules           règles de décision du tenant
     * @param on              date d'évaluation (pour le versionnement)
     * @return la décision de la première règle satisfaite, ou {@code null} si aucune
     */
    public static String evaluate(BigDecimal overallAverage, Map<Long, BigDecimal> subjectAverages,
                                  List<DecisionRule> rules, LocalDate on) {
        if (rules == null) return null;
        Map<Long, BigDecimal> subjects = subjectAverages != null ? subjectAverages : Map.of();
        return rules.stream()
                .filter(DecisionRule::isActive)
                .filter(r -> isValidOn(r, on))
                .sorted(Comparator.comparingInt(DecisionRule::getPriority))
                .filter(r -> matches(r, overallAverage, subjects))
                .map(DecisionRule::getDecision)
                .findFirst()
                .orElse(null);
    }

    private static boolean isValidOn(DecisionRule r, LocalDate on) {
        if (on == null) return true;
        if (r.getValidFrom() != null && on.isBefore(r.getValidFrom())) return false;
        return r.getValidTo() == null || !on.isAfter(r.getValidTo());
    }

    private static boolean matches(DecisionRule r, BigDecimal overall, Map<Long, BigDecimal> subjects) {
        BigDecimal threshold = r.getThreshold();
        return switch (r.getConditionType()) {
            case OVERALL_AT_LEAST -> overall != null && threshold != null && overall.compareTo(threshold) >= 0;
            case OVERALL_BELOW -> overall != null && threshold != null && overall.compareTo(threshold) < 0;
            case SUBJECT_BELOW -> {
                BigDecimal avg = r.getSubjectId() == null ? null : subjects.get(r.getSubjectId());
                yield avg != null && threshold != null && avg.compareTo(threshold) < 0;
            }
            case SUBJECTS_BELOW_COUNT -> {
                if (threshold == null || r.getMinCount() == null) yield false;
                long count = subjects.values().stream().filter(v -> v != null && v.compareTo(threshold) < 0).count();
                yield count >= r.getMinCount();
            }
        };
    }
}
