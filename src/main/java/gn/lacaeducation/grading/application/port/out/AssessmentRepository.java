package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.Assessment;
import java.util.List;
import java.util.Optional;
public interface AssessmentRepository {
    Assessment save(Assessment assessment);
    Optional<Assessment> findById(Long id);
    List<Assessment> findByClassId(Long classId);
    List<Assessment> findByPeriodId(Long periodId);
    List<Assessment> findByClassIdAndPeriodId(Long classId, Long periodId);
    List<Assessment> findByExamId(Long examId);
    void deleteById(Long id);
}
