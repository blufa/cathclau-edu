package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.GradeHistory;

import java.util.List;

public interface GradeHistoryRepository {
    GradeHistory save(GradeHistory history);
    List<GradeHistory> findByGradeId(Long gradeId);
    List<GradeHistory> findByAssessmentId(Long assessmentId);
    List<GradeHistory> findByStudentId(Long studentId);
}
