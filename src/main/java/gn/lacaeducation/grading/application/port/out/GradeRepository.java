package gn.lacaeducation.grading.application.port.out;
import gn.lacaeducation.grading.domain.Grade;
import java.util.List;
import java.util.Optional;
public interface GradeRepository {
    Grade save(Grade grade);
    Optional<Grade> findById(Long id);
    List<Grade> findByAssessmentId(Long assessmentId);
    List<Grade> findByStudentId(Long studentId);
    void deleteById(Long id);
}
