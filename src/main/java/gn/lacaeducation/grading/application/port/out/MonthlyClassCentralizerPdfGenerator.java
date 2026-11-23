package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.application.port.out.ClassCentralizerPdfGenerator.SubjectColumn;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Centralisateur de classe pour un bulletin de contrôle mensuel (§censeur) — même gabarit visuel que
 * {@link ClassCentralizerPdfGenerator} (le centralisateur semestriel/annuel), mais un seul mois : pas
 * de split 1er/2ème semestre, une seule moyenne par matière et par élève. Les rangs affichés ne sont
 * jamais recalculés ici — ils reprennent tels quels ceux déjà posés par
 * {@code MonthlyReportCardService} (même règle d'ex æquo, voir RankingUtil), ce document n'est qu'une
 * mise en forme "classe entière" des bulletins mensuels individuels déjà générés.
 */
public interface MonthlyClassCentralizerPdfGenerator {

    byte[] generate(MonthlyCentralizer data);

    /** Un élève, une seule ligne de notes (pas de split semestre) — {@code averageBySubject} indexée
     * par {@code subjectId} (voir {@link SubjectColumn}), absente de la map si la matière n'a aucune
     * note mensuelle ce mois pour cet élève. */
    record MonthlyStudentBlock(
            Long studentId, String studentNumber, String studentName, String levelLabel,
            Map<Long, BigDecimal> averageBySubject,
            BigDecimal overallAverage, Integer rank) {
    }

    record MonthlyCentralizer(
            Long tenantId,
            String className,
            String levelLabel,
            String schoolYear,
            String monthLabel,
            List<SubjectColumn> subjects,
            List<MonthlyStudentBlock> rows) {

        public int studentCount() {
            return rows == null ? 0 : rows.size();
        }
    }
}
