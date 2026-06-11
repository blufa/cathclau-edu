package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.Exam;
import java.util.List;
import java.util.Optional;
public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(Long id);
    List<Exam> findByClassId(Long classId);
    List<Exam> findByPeriodId(Long periodId);
    void deleteById(Long id);
}
