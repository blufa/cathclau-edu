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
    void delete(Long id);
}
