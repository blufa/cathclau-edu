package gn.lacaeducation.grading.domain;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.SortedMap;

/** Détail mensuel "cours" d'une matière pour un élève (bulletin §colonnes par mois) — recalculé à la
 * volée par {@code GradeCalculationService.monthlyBreakdown}, jamais persisté. Toujours absent de la
 * liste en Primaire (pas de notion de "cours" dans ce cycle, voir {@link SubjectSemesterAverage}). */
public record SubjectMonthlyBreakdown(Long subjectId, BigDecimal coefficient,
        SortedMap<YearMonth, BigDecimal> monthlyCours, BigDecimal compoAverage, BigDecimal subjectAverage) {
}
