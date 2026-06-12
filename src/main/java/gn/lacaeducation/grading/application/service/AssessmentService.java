package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
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
    private final AcademicsDataProvider academicsDataProvider;

    @Override public Assessment create(CreateAssessmentCommand c) {
        requireOpenPeriod(c.periodId());
        return repo.save(Assessment.builder().tenantId(c.tenantId()).classId(c.classId()).subjectId(c.subjectId())
                .teacherId(c.teacherId()).periodId(c.periodId()).type(c.type()).label(c.label())
                .date(c.date()).coefficient(c.coefficient()).maxScore(c.maxScore()).build());
    }

    @Override public Assessment update(Long id, CreateAssessmentCommand c) {
        Assessment a = load(id);
        requireOpenPeriod(a.getPeriodId());
        if (c.periodId() != null) requireOpenPeriod(c.periodId());
        a.setClassId(c.classId()); a.setSubjectId(c.subjectId()); a.setTeacherId(c.teacherId());
        a.setPeriodId(c.periodId()); a.setType(c.type()); a.setLabel(c.label());
        a.setDate(c.date()); a.setCoefficient(c.coefficient()); a.setMaxScore(c.maxScore());
        return repo.save(a);
    }

    @Override @Transactional(readOnly=true) public Optional<Assessment> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Assessment> findByClass(Long c) { return repo.findByClassId(c); }
    @Override @Transactional(readOnly=true) public List<Assessment> findByPeriod(Long p) { return repo.findByPeriodId(p); }
    @Override public void delete(Long id) {
        Assessment a = load(id);
        requireOpenPeriod(a.getPeriodId());
        repo.deleteById(id);
    }

    /** Creating/modifying an assessment is forbidden on a closed period (or a non-EN_COURS year). */
    private void requireOpenPeriod(Long periodId) {
        if (periodId != null && !academicsDataProvider.isPeriodOpenForGrades(periodId)) {
            throw new BusinessRuleException("Period " + periodId + " is closed; assessments cannot be modified.");
        }
    }

    private Assessment load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assessment", id));
    }
}
