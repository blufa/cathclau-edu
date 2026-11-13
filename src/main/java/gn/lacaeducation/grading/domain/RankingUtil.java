package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Classement par moyenne décroissante, partagé par {@code GradeCalculationService} (bulletin
 * officiel) et {@code MonthlyReportCardService} (bulletin mensuel) — même règle d'ex æquo dans les
 * deux cas : deux moyennes identiques partagent le même rang, le suivant reprend au rang
 * correspondant à sa position réelle (ex. 1, 1, 3, 4, 4, 6). Un élève sans moyenne (aucune note) ne
 * partage jamais son rang, y compris avec un autre élève sans moyenne.
 */
public final class RankingUtil {

    private RankingUtil() {}

    public record Ranked<T>(T item, Integer rank) {}

    public static <T> List<Ranked<T>> rank(List<T> items, Function<T, BigDecimal> averageOf) {
        List<T> sorted = items.stream()
                .sorted(Comparator.comparing(averageOf, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        Map<T, Integer> ranks = new LinkedHashMap<>();
        int rank = 1;
        BigDecimal previousAverage = null;
        int position = 0;
        for (T item : sorted) {
            position++;
            BigDecimal current = averageOf.apply(item);
            if (previousAverage == null || current == null || previousAverage.compareTo(current) != 0) {
                rank = position;
            }
            ranks.put(item, rank);
            previousAverage = current;
        }
        return sorted.stream().map(item -> new Ranked<>(item, ranks.get(item))).toList();
    }
}
