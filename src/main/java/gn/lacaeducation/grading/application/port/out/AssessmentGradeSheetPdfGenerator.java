package gn.lacaeducation.grading.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Génère le PDF d'UNE évaluation précise (ex. « Anglais — Oral — Octobre ») : liste des élèves avec
 * leur note déjà saisie (ou une ligne vide si pas encore notée) — téléchargeable/partageable, à la
 * différence de la fiche de notes vierge multi-mois ({@link AssessmentPlanPdfGenerator}).
 */
public interface AssessmentGradeSheetPdfGenerator {

    byte[] generate(AssessmentGradeSheet sheet);

    record AssessmentGradeSheet(Long tenantId, String className, String subjectName, String assessmentLabel,
                                String assessmentTypeLabel, LocalDate date, BigDecimal maxScore,
                                List<StudentGradeRow> rows) {}

    record StudentGradeRow(String studentName, BigDecimal value) {}
}
