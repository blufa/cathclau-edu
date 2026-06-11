package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class GradePersistenceAdapter implements GradeRepository {
    private final GradeJpaRepository jpa;
    private final GradingMapper mapper;
    @Override public Grade save(Grade g) { return mapper.toDomain(jpa.save(mapper.toEntity(g))); }
    @Override public List<Grade> saveAll(List<Grade> grades) {
        List<GradeJpaEntity> entities = grades.stream().map(mapper::toEntity).toList();
        return jpa.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
    @Override public Optional<Grade> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Grade> findByAssessmentId(Long id) { return jpa.findByAssessmentId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Grade> findByStudentId(Long id) { return jpa.findByStudentId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Grade> findByStudentIdAndPeriodId(Long studentId, Long periodId) {
        return jpa.findByStudentIdAndPeriodId(studentId, periodId).stream().map(mapper::toDomain).toList();
    }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
