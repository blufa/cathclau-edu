package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class ReportCardService implements ReportCardUseCase {
    private final ReportCardRepository repo;
    private final GradeRepository gradeRepo;
    private final ApplicationEventPublisher eventPublisher;
    @Override
    public ReportCard generate(GenerateReportCardCommand c) {
        return repo.save(ReportCard.builder().studentId(c.studentId()).classId(c.classId())
                .periodId(c.periodId()).status(ReportCardStatus.DRAFT).generatedAt(LocalDateTime.now()).build());
    }
    @Override
    public ReportCard publish(Long id) {
        ReportCard rc = load(id);
        rc.setStatus(ReportCardStatus.PUBLISHED);
        ReportCard saved = repo.save(rc);
        eventPublisher.publishEvent(new ReportCardPublishedEvent(null, rc.getStudentId(), rc.getPeriodId(), rc.getId()));
        return saved;
    }
    @Override @Transactional(readOnly=true) public Optional<ReportCard> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<ReportCard> findByClass(Long c, Long p) { return repo.findByClassIdAndPeriodId(c, p); }
    @Override @Transactional(readOnly=true) public Optional<ReportCard> findByStudent(Long s, Long p) { return repo.findByStudentIdAndPeriodId(s, p); }
    @Override @Transactional(readOnly=true) public List<ReportCardLine> findLines(Long id) { return repo.findLines(id); }
    @Override public void delete(Long id) { repo.deleteById(id); }
    private ReportCard load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Report card not found: " + id)); }
}
