package gn.lacaeducation.grading.adapters.out.persistence;

import gn.lacaeducation.grading.application.port.out.GradeHistoryRepository;
import gn.lacaeducation.grading.domain.GradeHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class GradeHistoryPersistenceAdapter implements GradeHistoryRepository {

    private final GradeHistoryJpaRepository jpa;
    private final GradingMapper mapper;

    @Override
    public GradeHistory save(GradeHistory history) {
        return mapper.toDomain(jpa.save(mapper.toEntity(history)));
    }

    @Override
    public List<GradeHistory> findByGradeId(Long gradeId) {
        return jpa.findByGradeIdOrderByChangedAtDesc(gradeId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GradeHistory> findByAssessmentId(Long assessmentId) {
        return jpa.findByAssessmentIdOrderByChangedAtDesc(assessmentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GradeHistory> findByStudentId(Long studentId) {
        return jpa.findByStudentIdOrderByChangedAtDesc(studentId).stream().map(mapper::toDomain).toList();
    }
}
