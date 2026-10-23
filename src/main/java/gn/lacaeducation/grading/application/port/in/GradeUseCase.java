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
    /** Une évaluation (colonne du relevé) avec son barème. {@code type} = {@code AssessmentType} brut
     * (QUIZ/HOMEWORK/EXAM/FINAL), utilisé UNIQUEMENT pour répartir les colonnes entre Évaluations et
     * Compositions (voir {@link ReleveSection}) — jamais affiché tel quel : le relevé affiche toujours
     * {@code label}, le texte réellement saisi par l'enseignant (§retour utilisateur, "ce n'est pas le
     * type"). {@code coefficient} = coefficient par évaluation, déprécié pour le calcul de la moyenne
     * (voir {@code SubjectSemesterAverage}) — gardé seulement pour affichage historique, jamais utilisé
     * pour la "note coefciée" (qui applique {@link SubjectGradeReport#subjectCoefficient}). */
    record ReleveAssessment(Long id, String label, String type, LocalDate date, BigDecimal maxScore, BigDecimal coefficient) {}
    /** Une ligne élève : sa note à chaque évaluation de la section (map assessmentId → note) et sa
     * moyenne de section (M. Cours pour les Évaluations, M. Composition pour les Compositions — déjà
     * "coefciée" par {@code SubjectSemesterAverage}, jamais recalculée côté PDF). */
    record ReleveRow(Long studentId, String studentNumber, String fullName, Map<Long, BigDecimal> notes, BigDecimal average) {}
    /** Une section du relevé (Évaluations OU Compositions) : ses colonnes et ses lignes élèves — jamais
     * mélangées entre les deux catégories (§retour utilisateur, "il doit avoir les évaluations à part,
     * et une liste des compositions"). */
    record ReleveSection(List<ReleveAssessment> assessments, List<ReleveRow> rows) {}
    /**
     * Relevé de notes d'une matière pour une classe et une période : les évaluations "cours"
     * (interrogations/devoirs) et les compositions dans deux sections distinctes, jamais une colonne
     * commune. {@code subjectCoefficient} : coefficient configuré de la matière (Classes → Matières),
     * affiché une fois (carte d'en-tête du PDF) et utilisé pour la "note coefciée" sous chaque note —
     * les enseignants ne saisissent que la note brute, le coefficient est appliqué par le système.
     */
    record SubjectGradeReport(Long tenantId, Long classId, Long subjectId, Long periodId,
                              String className, String subjectName, String periodLabel, String schoolYear,
                              BigDecimal subjectCoefficient, ReleveSection evaluations, ReleveSection compositions) {}

    SubjectGradeReport subjectGradeReport(Long classId, Long subjectId, Long periodId);
    /** @param month si non nul (1-12), limite le PDF aux évaluations de ce mois (relevé mensuel). */
    byte[] subjectGradeReportPdf(Long classId, Long subjectId, Long periodId, Integer month);

    List<Grade> findByStudent(Long studentId);
    List<Grade> findByStudentAndPeriod(Long studentId, Long periodId);

    /** Full modification history of a single grade, most recent first. */
    List<GradeHistory> findHistory(Long gradeId);
}
