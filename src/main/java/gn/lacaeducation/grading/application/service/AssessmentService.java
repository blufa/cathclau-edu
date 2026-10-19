package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.event.AssessmentScheduledEvent;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class AssessmentService implements AssessmentUseCase {
    private final AssessmentRepository repo;
    private final AcademicsDataProvider academicsDataProvider;
    private final StudentDataProvider studentDataProvider;
    private final GradeRepository gradeRepository;
    private final gn.lacaeducation.grading.application.port.out.AssessmentPlanPdfGenerator planPdfGenerator;
    private final gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator gradeSheetPdfGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Override public Assessment create(CreateAssessmentCommand c) {
        requireOpenPeriod(c.periodId());

        // Le front n'envoie pas le tenant : on le résout depuis le contexte (multi-tenant).
        Long tenantId = c.tenantId() != null ? c.tenantId() : gn.lacaeducation.common.TenantContext.get();
        // Coefficient : celui saisi, sinon 1 par défaut — standard, non paramétrable par établissement
        // (§retour pédagogique, plus de catalogue de types d'évaluation personnalisés).
        BigDecimal coefficient = c.coefficient() != null ? c.coefficient() : BigDecimal.ONE;
        Assessment saved = repo.save(Assessment.builder().tenantId(tenantId).classId(c.classId()).subjectId(c.subjectId())
                .teacherId(c.teacherId()).periodId(c.periodId()).type(c.type()).assessmentTypeId(c.assessmentTypeId())
                .label(c.label()).date(c.date()).coefficient(coefficient).maxScore(c.maxScore()).build());
        // Durée de la séance comptabilisée : standard par type d'évaluation (§retour pédagogique, plus
        // de réglage par établissement — voir AssessmentType#sessionMinutes).
        Integer sessionMinutes = saved.getType() != null
                ? saved.getType().sessionMinutes()
                : gn.lacaeducation.grading.domain.AssessmentType.HOMEWORK.sessionMinutes();
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

    @Override
    @Transactional(readOnly = true)
    public byte[] planPdf(Long classId, Long subjectId, Long academicYearId) {
        Long tenantId = gn.lacaeducation.common.TenantContext.get();
        String className = academicsDataProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String subjectName = academicsDataProvider.subjectLabels(java.util.Set.of(subjectId)).get(subjectId);
        String schoolYear = tenantId == null ? null : academicsDataProvider.currentAcademicYearLabel(tenantId).orElse(null);
        List<String> studentNames = studentDataProvider.findStudentsByClass(classId).stream()
                .sorted(java.util.Comparator.comparing(StudentPublicInfo::lastName,
                        java.util.Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(s -> ((s.firstName() != null ? s.firstName() : "") + " " + (s.lastName() != null ? s.lastName() : "")).trim())
                .toList();
        return planPdfGenerator.generate(new gn.lacaeducation.grading.application.port.out.AssessmentPlanPdfGenerator.AssessmentPlan(
                tenantId, className, subjectName, schoolYear, studentNames));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] assessmentGradeSheetPdf(Long assessmentId) {
        Assessment a = load(assessmentId);
        String className = academicsDataProvider.findClass(a.getClassId())
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String subjectName = academicsDataProvider.subjectLabels(java.util.Set.of(a.getSubjectId())).get(a.getSubjectId());
        String typeLabel = a.getType().name();
        Map<Long, BigDecimal> gradeByStudent = gradeRepository.findByAssessmentId(assessmentId).stream()
                .collect(java.util.stream.Collectors.toMap(Grade::getStudentId, Grade::getValue, (x, y) -> x));
        List<gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator.StudentGradeRow> rows =
                studentDataProvider.findStudentsByClass(a.getClassId()).stream()
                        .sorted(java.util.Comparator.comparing(StudentPublicInfo::lastName,
                                java.util.Comparator.nullsLast(String::compareToIgnoreCase)))
                        .map(s -> new gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator.StudentGradeRow(
                                ((s.firstName() != null ? s.firstName() : "") + " " + (s.lastName() != null ? s.lastName() : "")).trim(),
                                s.studentNumber(),
                                gradeByStudent.get(s.id())))
                        .toList();
        return gradeSheetPdfGenerator.generate(new gn.lacaeducation.grading.application.port.out.AssessmentGradeSheetPdfGenerator.AssessmentGradeSheet(
                a.getTenantId(), className, subjectName, a.getLabel(), typeLabel, a.getDate(), a.getMaxScore(), rows));
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
