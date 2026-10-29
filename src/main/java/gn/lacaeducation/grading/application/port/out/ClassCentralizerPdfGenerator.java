package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Centralisateur de classe (§documents ministère, maquette de référence {@code features/
 * Centralisateur-bon-design.pdf}) : une ligne par élève, moyennes générales mensuelles (toutes
 * matières confondues, pondérées par coefficient — jamais une moyenne par matière) par semestre,
 * puis le bilan annuel (moyenne, rang, mention, décision) — même agrégation que le bulletin annuel
 * ({@code AnnualReportCardUseCase}), jamais recalculée différemment pour éviter tout écart entre les
 * deux documents.
 */
public interface ClassCentralizerPdfGenerator {

    byte[] generate(Centralizer data);

    record StudentRow(
            String studentName, String studentNumber,
            Map<YearMonth, BigDecimal> s1Monthly, BigDecimal s1Compo, BigDecimal s1Average,
            Map<YearMonth, BigDecimal> s2Monthly, BigDecimal s2Compo, BigDecimal s2Average,
            BigDecimal annualAverage, Integer rank, String honor, String decision) {
    }

    record Centralizer(
            Long tenantId,
            String className,
            String schoolYear,
            List<YearMonth> s1Months,
            List<YearMonth> s2Months,
            List<StudentRow> rows,
            BigDecimal s1ClassAverage,
            BigDecimal s2ClassAverage,
            BigDecimal annualClassAverage,
            int admittedCount,
            String topStudentName) {

        public int studentCount() {
            return rows == null ? 0 : rows.size();
        }
    }
}
