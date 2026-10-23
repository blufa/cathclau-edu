package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface ExamUseCase {
    record CreateExamCommand(Long tenantId, Long classId, Long periodId, ExamType type, String label, LocalDate scheduledDate) {}
    Exam create(CreateExamCommand command);
    Optional<Exam> findById(Long id);
    List<Exam> findByClass(Long classId);
    List<Exam> findByPeriod(Long periodId);
    Exam start(Long id);
    Exam complete(Long id);
    Optional<String> getConvocation(Long id);
    /** Génère et renvoie les octets du PDF de la convocation (affichage direct, sans Cloudinary). */
    byte[] convocationBytes(Long id);
    void generateConvocationAsync(Long id);
    void delete(Long id);

    // ── Surveillance (composition : surveillants par salle) ──────────────────────
    ExamSupervision assignSupervisor(Long examId, Long roomId, Long teacherId);
    List<ExamSupervision> supervisions(Long examId);
    void removeSupervision(Long supervisionId);

    // ── Saisie des notes par matière (§retour utilisateur : la composition se note depuis cet
    // onglet, jamais via "Nouvelle évaluation") ───────────────────────────────────
    /** Évaluations (une par matière déjà notée) liées à cette composition — pour afficher quelles
     * matières ont déjà des notes saisies. */
    List<Assessment> assessmentsByExam(Long examId);
    /** Retrouve l'évaluation de cette matière pour cette composition, ou la crée (type EXAM) si
     * c'est la première saisie — jamais de doublon. Ne touche à aucun calcul de moyenne : c'est un
     * {@code Assessment} normal, traité ensuite exactement comme les autres. */
    Assessment gradeEntryAssessment(Long examId, Long subjectId);
}
