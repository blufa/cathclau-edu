package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.event.ReportCardPublishedEvent;
import gn.lacaeducation.grading.application.port.in.ReportCardUseCase;
import gn.lacaeducation.grading.application.port.out.ReportCardRepository;
import gn.lacaeducation.grading.domain.ReportCard;
import gn.lacaeducation.grading.domain.ReportCardLine;
import gn.lacaeducation.grading.domain.ReportCardStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportCardService implements ReportCardUseCase {

    private final ReportCardRepository repo;
    private final GradeCalculationService calculationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ReportCard generate(GenerateReportCardCommand c) {
        return calculationService.generateForStudent(c.studentId(), c.classId(), c.periodId());
    }

    @Override
    public List<ReportCard> generateBulk(GenerateBulkCommand c) {
        return calculationService.generateForClass(c.classId(), c.periodId());
    }

    @Override
    public ReportCard publish(Long id) {
        ReportCard rc = load(id);
        rc.setStatus(ReportCardStatus.PUBLISHED);
        ReportCard saved = repo.save(rc);
        eventPublisher.publishEvent(
                new ReportCardPublishedEvent(null, rc.getStudentId(), rc.getPeriodId(), rc.getId()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportCard> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCard> findByClass(Long classId, Long periodId) {
        return repo.findByClassIdAndPeriodId(classId, periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReportCard> findByStudent(Long studentId, Long periodId) {
        return repo.findByStudentIdAndPeriodId(studentId, periodId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCard> findByStudentId(Long studentId) {
        return repo.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardLine> findLines(Long id) {
        return repo.findLines(id);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private ReportCard load(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReportCard", id));
    }
}
