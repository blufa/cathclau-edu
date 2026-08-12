package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Génère le PDF du centralisateur de classe (§documents ministère) : une ligne par élève, une colonne
 * Cours/Composition par matière, moyenne générale et rang — reprend les {@code ReportCard}/
 * {@code ReportCardLine} déjà calculés, aucun nouveau calcul.
 */
public interface ClassCentralizerPdfGenerator {

    byte[] generate(Centralizer data);

    record SubjectCell(BigDecimal cours, BigDecimal compo) {}

    record StudentRow(String studentName, String studentNumber, Map<Long, SubjectCell> bySubject,
                      BigDecimal overallAverage, Integer rank) {}

    record Centralizer(
            Long tenantId,
            String className,
            String periodLabel,
            String schoolYear,
            List<Long> subjectIds,
            Map<Long, String> subjectNames,
            Map<Long, BigDecimal> subjectCoefficients,
            List<StudentRow> rows) {

        public String subjectName(Long subjectId) {
            String name = subjectNames == null ? null : subjectNames.get(subjectId);
            return name != null && !name.isBlank() ? name : "Matière " + subjectId;
        }
    }
}
