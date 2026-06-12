package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.event.GradesValidatedEvent;
import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class GradeService implements GradeUseCase {

    private final GradeRepository repo;
    private final AssessmentRepository assessmentRepository;
    private final AcademicsDataProvider academicsDataProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Grade enter(EnterGradeCommand c) {
        Assessment assessment = requireOpenPeriod(c.assessmentId());
        validateValue(c.value(), assessment);
        return repo.save(Grade.builder()
                .assessmentId(c.assessmentId())
                .studentId(c.studentId())
                .value(c.value())
                .validated(false)
                .enteredAt(LocalDateTime.now())
                .build());
    }

    @Override
    public List<Grade> enterBulk(Long assessmentId, List<BulkGradeEntry> entries) {
        Assessment assessment = requireOpenPeriod(assessmentId);
        entries.forEach(e -> validateValue(e.value(), assessment));
        List<Grade> grades = entries.stream()
                .map(e -> Grade.builder()
                        .assessmentId(assessmentId)
                        .studentId(e.studentId())
                        .value(e.value())
                        .validated(false)
                        .enteredAt(LocalDateTime.now())
                        .build())
                .toList();
        return repo.saveAll(grades);
    }

    @Override
    public List<Grade> validateAllForAssessment(Long assessmentId) {
        List<Grade> grades = repo.findByAssessmentId(assessmentId);
        grades.forEach(g -> g.setValidated(true));
        List<Grade> saved = repo.saveAll(grades);
        eventPublisher.publishEvent(new GradesValidatedEvent(assessmentId));
        return saved;
    }

    @Override
    public Grade update(Long id, BigDecimal value) {
        Grade g = load(id);
        if (Boolean.TRUE.equals(g.getValidated())) {
            throw new BusinessRuleException("Grade " + id + " is validated (locked); unlock it before correcting.");
        }
        Assessment assessment = requireOpenPeriod(g.getAssessmentId());
        validateValue(value, assessment);
        g.setValue(value);
        return repo.save(g);
    }

    @Override
    public Grade validate(Long id) {
        Grade g = load(id);
        g.setValidated(true);
        Grade saved = repo.save(g);
        eventPublisher.publishEvent(new GradesValidatedEvent(g.getAssessmentId()));
        return saved;
    }

    @Override
    public Grade unlock(Long id) {
        Grade g = load(id);
        g.setValidated(false);
        return repo.save(g);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Grade> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByAssessment(Long a) {
        return repo.findByAssessmentId(a);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByStudent(Long s) {
        return repo.findByStudentId(s);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByStudentAndPeriod(Long studentId, Long periodId) {
        return repo.findByStudentIdAndPeriodId(studentId, periodId);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /** Grade entry/correction is allowed only on a non-closed period of an EN_COURS year. */
    private Assessment requireOpenPeriod(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", assessmentId));
        if (!academicsDataProvider.isPeriodOpenForGrades(assessment.getPeriodId())) {
            throw new BusinessRuleException(
                    "Grades can only be entered on an open period of an in-progress academic year.");
        }
        return assessment;
    }

    /** 0 <= value <= assessment.maxScore. */
    private void validateValue(BigDecimal value, Assessment assessment) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Grade value must be >= 0.");
        }
        BigDecimal max = assessment.getMaxScore() != null ? assessment.getMaxScore() : BigDecimal.valueOf(20);
        if (value.compareTo(max) > 0) {
            throw new BusinessRuleException("Grade value " + value + " exceeds the maximum score " + max + ".");
        }
    }

    private Grade load(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", id));
    }
}
