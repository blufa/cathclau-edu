package gn.lacaeducation.grading.adapters.out;

import gn.lacaeducation.grading.SchoolAcademicStatsProvider;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Calcule les agrégats académiques d'une période à partir des bulletins (§10.1). */
@Component
@RequiredArgsConstructor
class SchoolAcademicStatsProviderAdapter implements SchoolAcademicStatsProvider {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReportCardRepository reportCardRepository;

    @Override
    @Transactional(readOnly = true)
    public AcademicStats academicStats(Long periodId, int topLimit) {
        List<ReportCard> cards = reportCardRepository.findByPeriodId(periodId);
        if (cards.isEmpty()) {
            return new AcademicStats(periodId, 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        }

        BigDecimal overallAverage = mean(cards.stream().map(ReportCard::getOverallAverage).toList());
        BigDecimal passRate = passRate(cards);

        Map<Long, List<ReportCard>> byClass = cards.stream()
                .filter(rc -> rc.getClassId() != null)
                .collect(Collectors.groupingBy(ReportCard::getClassId));
        List<ClassStat> classStats = byClass.entrySet().stream()
                .map(e -> {
                    List<ReportCard> cs = e.getValue();
                    List<BigDecimal> avgs = cs.stream().map(ReportCard::getOverallAverage).filter(v -> v != null).toList();
                    return new ClassStat(e.getKey(), cs.size(), mean(avgs),
                            max(avgs), min(avgs), passRate(cs));
                })
                .sorted(Comparator.comparing(ClassStat::classId))
                .toList();

        int limit = topLimit > 0 ? topLimit : 10;
        List<RankedStudent> topStudents = cards.stream()
                .filter(rc -> rc.getOverallAverage() != null)
                .sorted(Comparator.comparing(ReportCard::getOverallAverage).reversed())
                .limit(limit)
                .map(rc -> new RankedStudent(rc.getStudentId(), rc.getClassId(), rc.getOverallAverage(), rc.getRank()))
                .toList();

        return new AcademicStats(periodId, cards.size(), overallAverage, passRate, classStats, topStudents);
    }

    /** Taux de réussite = part de décisions « ADMIS » (en %, 2 décimales). */
    private BigDecimal passRate(List<ReportCard> cards) {
        long decided = cards.stream().filter(rc -> rc.getDecision() != null && !rc.getDecision().isBlank()).count();
        if (decided == 0) return BigDecimal.ZERO;
        long admitted = cards.stream().filter(rc -> "ADMIS".equalsIgnoreCase(rc.getDecision())).count();
        return BigDecimal.valueOf(admitted).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(decided), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal mean(List<BigDecimal> values) {
        List<BigDecimal> nonNull = values.stream().filter(v -> v != null).toList();
        if (nonNull.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(List<BigDecimal> values) {
        return values.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    private BigDecimal min(List<BigDecimal> values) {
        return values.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }
}
