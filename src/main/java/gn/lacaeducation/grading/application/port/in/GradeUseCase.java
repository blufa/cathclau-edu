package gn.lacaeducation.grading.application.port.in;
import gn.lacaeducation.grading.domain.Grade;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface GradeUseCase {
    record EnterGradeCommand(Long assessmentId, Long studentId, BigDecimal value) {}
    record BulkGradeEntry(Long studentId, BigDecimal value) {}
    Grade enter(EnterGradeCommand command);
    List<Grade> enterBulk(Long assessmentId, List<BulkGradeEntry> entries);
    List<Grade> validateAllForAssessment(Long assessmentId);
    Grade update(Long id, BigDecimal value);
    Grade validate(Long id);
    Optional<Grade> findById(Long id);
    List<Grade> findByAssessment(Long assessmentId);
    List<Grade> findByStudent(Long studentId);
    List<Grade> findByStudentAndPeriod(Long studentId, Long periodId);
    void delete(Long id);
}
