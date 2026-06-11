package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.domain.Assessment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class AssessmentPersistenceAdapter implements AssessmentRepository {
    private final AssessmentJpaRepository jpa;
    private final GradingMapper mapper;
    @Override public Assessment save(Assessment a) { return mapper.toDomain(jpa.save(mapper.toEntity(a))); }
    @Override public Optional<Assessment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Assessment> findByClassId(Long id) { return jpa.findByClassId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Assessment> findByPeriodId(Long id) { return jpa.findByPeriodId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Assessment> findByClassIdAndPeriodId(Long classId, Long periodId) { return jpa.findByClassIdAndPeriodId(classId, periodId).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
