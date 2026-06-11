package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.Grade;
import java.util.List;
import java.util.Optional;
public interface GradeRepository {
    Grade save(Grade grade);
    List<Grade> saveAll(List<Grade> grades);
    Optional<Grade> findById(Long id);
    List<Grade> findByAssessmentId(Long assessmentId);
    List<Grade> findByStudentId(Long studentId);
    List<Grade> findByStudentIdAndPeriodId(Long studentId, Long periodId);
    void deleteById(Long id);
}
