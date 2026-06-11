package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.domain.Assessment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class AssessmentService implements AssessmentUseCase {
    private final AssessmentRepository repo;
    @Override public Assessment create(CreateAssessmentCommand c) {
        return repo.save(Assessment.builder().tenantId(c.tenantId()).classId(c.classId()).subjectId(c.subjectId())
                .teacherId(c.teacherId()).periodId(c.periodId()).type(c.type()).label(c.label())
                .date(c.date()).coefficient(c.coefficient()).maxScore(c.maxScore()).build());
    }
    @Override @Transactional(readOnly=true) public Optional<Assessment> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Assessment> findByClass(Long c) { return repo.findByClassId(c); }
    @Override @Transactional(readOnly=true) public List<Assessment> findByPeriod(Long p) { return repo.findByPeriodId(p); }
    @Override public void delete(Long id) { repo.deleteById(id); }
}
