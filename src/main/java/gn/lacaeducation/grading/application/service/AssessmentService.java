package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.event.AssessmentScheduledEvent;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.AssessmentSessionSettingsRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentSessionSettings;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class AssessmentService implements AssessmentUseCase {
    private final AssessmentRepository repo;
    private final AcademicsDataProvider academicsDataProvider;
    private final AssessmentSessionSettingsRepository sessionSettingsRepository;
    private final gn.lacaeducation.grading.application.port.out.AssessmentTypeConfigRepository assessmentTypeConfigRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override public Assessment create(CreateAssessmentCommand c) {
        requireOpenPeriod(c.periodId());

        // Le front n'envoie pas le tenant : on le résout depuis le contexte (multi-tenant).
        Long tenantId = c.tenantId() != null ? c.tenantId() : gn.lacaeducation.common.TenantContext.get();
        // Coefficient : celui saisi, sinon le coefficient par défaut du type configurable choisi (§3).
        BigDecimal coefficient = c.coefficient();
        if (coefficient == null && c.assessmentTypeId() != null) {
            coefficient = assessmentTypeConfigRepository.findById(c.assessmentTypeId())
                    .map(gn.lacaeducation.grading.domain.AssessmentTypeConfig::getDefaultCoefficient)
                    .orElse(null);
        }
        Assessment saved = repo.save(Assessment.builder().tenantId(tenantId).classId(c.classId()).subjectId(c.subjectId())
                .teacherId(c.teacherId()).periodId(c.periodId()).type(c.type()).assessmentTypeId(c.assessmentTypeId())
                .label(c.label()).date(c.date()).coefficient(coefficient).maxScore(c.maxScore()).build());
        // Durée de la séance comptabilisée : paramétrable par établissement selon le type d'évaluation.
        Integer sessionMinutes = sessionSettingsRepository.findByTenantId(tenantId)
                .orElseGet(AssessmentSessionSettings::defaults)
                .minutesFor(saved.getType());
        eventPublisher.publishEvent(new AssessmentScheduledEvent(saved.getTenantId(), saved.getId(),
                saved.getClassId(), saved.getSubjectId(), saved.getTeacherId(), saved.getLabel(),
                saved.getDate(), sessionMinutes));
        return saved;
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
