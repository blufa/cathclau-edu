package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Génère le PDF du bulletin de contrôle mensuel (§censeur) — diagnostic simple, pas le bulletin officiel. */
public interface MonthlyReportCardPdfGenerator {

    byte[] generate(MonthlyReportCardDocument doc);

    record MonthlyReportCardDocument(Long tenantId, String studentName, String studentNumber,
                                     String className, String monthLabel,
                                     BigDecimal overallAverage, List<Line> lines,
                                     String autoAppreciation, String comment) {}

    record Line(String subjectName, BigDecimal average, BigDecimal coefficient, String teacherName) {}
}
