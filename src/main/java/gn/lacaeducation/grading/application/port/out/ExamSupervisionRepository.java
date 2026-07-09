package gn.lacaeducation.grading.application.port.out;

import gn.lacaeducation.grading.domain.ExamSupervision;

import java.util.List;

public interface ExamSupervisionRepository {
    ExamSupervision save(ExamSupervision s);
    List<ExamSupervision> findByExamId(Long examId);
    List<ExamSupervision> findByTeacherId(Long teacherId);
    boolean existsByExamIdAndTeacherId(Long examId, Long teacherId);
    void deleteById(Long id);
}
