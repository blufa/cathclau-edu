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
}
