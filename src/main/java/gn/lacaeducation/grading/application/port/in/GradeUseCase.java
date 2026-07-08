package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.GradeHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public interface GradeUseCase {
    record EnterGradeCommand(Long assessmentId, Long studentId, BigDecimal value) {}
    record BulkGradeEntry(Long studentId, BigDecimal value) {}

    /**
     * Ligne de la feuille de notes d'une évaluation : un élève de la classe, avec sa note saisie
     * ({@code value}/{@code validated}/{@code gradeId} à {@code null} si aucune note encore saisie).
     */
    record GradeSheetRow(Long studentId, String studentNumber, String fullName,
                         Long gradeId, BigDecimal value, Boolean validated) {}

    Grade enter(EnterGradeCommand command, Long changedBy);
    List<Grade> enterBulk(Long assessmentId, List<BulkGradeEntry> entries, boolean allowOverwrite, Long changedBy);
    List<Grade> validateAllForAssessment(Long assessmentId, Long changedBy);
    Grade update(Long id, BigDecimal value, Long changedBy, String reason);
    Grade validate(Long id, Long changedBy);
    Grade unlock(Long id, Long changedBy);
    void delete(Long id, Long changedBy);

    // Convenience overloads (system/unauthenticated callers, tests) — no actor/reason recorded.
    default Grade enter(EnterGradeCommand command) { return enter(command, null); }
    default List<Grade> enterBulk(Long assessmentId, List<BulkGradeEntry> entries, boolean allowOverwrite) {
        return enterBulk(assessmentId, entries, allowOverwrite, null);
    }
    default List<Grade> validateAllForAssessment(Long assessmentId) { return validateAllForAssessment(assessmentId, null); }
    default Grade update(Long id, BigDecimal value) { return update(id, value, null, null); }
    default Grade validate(Long id) { return validate(id, null); }
    default Grade unlock(Long id) { return unlock(id, null); }
    default void delete(Long id) { delete(id, null); }

    Optional<Grade> findById(Long id);
    List<Grade> findByAssessment(Long assessmentId);

    /**
     * Feuille de notes d'une évaluation : tous les élèves de la classe concernée avec leur note saisie
     * (ou vide). Destinée à la vue « liste des élèves avec leurs notes » côté administration.
     */
    List<GradeSheetRow> gradeSheet(Long assessmentId);

    // ── Relevé de notes par matière (classe + matière + période) ─────────────────
    /** Une évaluation (colonne du relevé) avec son barème et son coefficient. */
    record ReleveAssessment(Long id, String label, String type, LocalDate date, BigDecimal maxScore, BigDecimal coefficient) {}
    /** Une ligne élève : sa note à chaque évaluation (map assessmentId → note) et sa moyenne calculée. */
    record ReleveRow(Long studentId, String studentNumber, String fullName, Map<Long, BigDecimal> notes, BigDecimal average) {}
    /**
     * Relevé de notes d'une matière pour une classe et une période : chaque évaluation en colonne
     * (éval 1, éval 2, …) plus la moyenne calculée selon le paramétrage de l'établissement. Se complète
     * au fur et à mesure que l'enseignant ajoute des évaluations.
     */
    record SubjectGradeReport(Long tenantId, Long classId, Long subjectId, Long periodId,
                              String className, String subjectName, String periodLabel, String schoolYear,
                              List<ReleveAssessment> assessments, List<ReleveRow> rows) {}

    SubjectGradeReport subjectGradeReport(Long classId, Long subjectId, Long periodId);
    byte[] subjectGradeReportPdf(Long classId, Long subjectId, Long periodId);

    List<Grade> findByStudent(Long studentId);
    List<Grade> findByStudentAndPeriod(Long studentId, Long periodId);

    /** Full modification history of a single grade, most recent first. */
    List<GradeHistory> findHistory(Long gradeId);
}
