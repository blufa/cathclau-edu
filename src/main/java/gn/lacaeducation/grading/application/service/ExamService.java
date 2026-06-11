package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.grading.application.port.in.ExamUseCase;
import gn.lacaeducation.grading.application.port.out.ExamRepository;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class ExamService implements ExamUseCase {
    private final ExamRepository repo;
    @Override public Exam create(CreateExamCommand c) {
        return repo.save(Exam.builder().tenantId(c.tenantId()).classId(c.classId()).periodId(c.periodId())
                .type(c.type()).label(c.label()).scheduledDate(c.scheduledDate()).status(ExamStatus.PLANNED).build());
    }
    @Override @Transactional(readOnly=true) public Optional<Exam> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Exam> findByClass(Long c) { return repo.findByClassId(c); }
    @Override @Transactional(readOnly=true) public List<Exam> findByPeriod(Long p) { return repo.findByPeriodId(p); }
    @Override public Exam start(Long id) { Exam e = load(id); e.setStatus(ExamStatus.ONGOING); return repo.save(e); }
    @Override public Exam complete(Long id) { Exam e = load(id); e.setStatus(ExamStatus.COMPLETED); return repo.save(e); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private Exam load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Exam not found: " + id)); }
}
