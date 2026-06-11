package gn.lacaeducation.grading.adapters.out.persistence;
import gn.lacaeducation.grading.application.port.out.ExamRepository;
import gn.lacaeducation.grading.domain.Exam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class ExamPersistenceAdapter implements ExamRepository {
    private final ExamJpaRepository jpa;
    private final GradingMapper mapper;
    @Override public Exam save(Exam e) { return mapper.toDomain(jpa.save(mapper.toEntity(e))); }
    @Override public Optional<Exam> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Exam> findByClassId(Long id) { return jpa.findByClassId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<Exam> findByPeriodId(Long id) { return jpa.findByPeriodId(id).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
