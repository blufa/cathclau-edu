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
        requireNoDuplicate(c.classId(), c.subjectId(), c.periodId(), c.type(), c.label(), c.date(), null);

        // Le front n'envoie pas le tenant : on le résout depuis le contexte (multi-tenant).
        Long tenantId = c.tenantId() != null ? c.tenantId() : gn.lacaeducation.common.TenantContext.get();
        // Coefficient : celui saisi, sinon 1 par défaut — standard, non paramétrable par établissement
        // (§retour pédagogique, plus de catalogue de types d'évaluation personnalisés).
        BigDecimal coefficient = c.coefficient() != null ? c.coefficient() : BigDecimal.ONE;
        Assessment saved = repo.save(Assessment.builder().tenantId(tenantId).classId(c.classId()).subjectId(c.subjectId())
                .teacherId(c.teacherId()).periodId(c.periodId()).type(c.type()).assessmentTypeId(c.assessmentTypeId())
                .label(c.label()).date(c.date()).coefficient(coefficient).maxScore(c.maxScore()).examId(c.examId()).build());
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
        requireNoDuplicate(c.classId(), c.subjectId(), c.periodId(), c.type(), c.label(), c.date(), id);
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
        gradeRepository.deleteByAssessmentId(id);
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DuplicateGroup> findDuplicateQuizGroups() {
        Long tenantId = gn.lacaeducation.common.TenantContext.get();
        Map<String, List<Assessment>> byKey = repo.findByTenantId(tenantId).stream()
                .filter(a -> a.getType() == gn.lacaeducation.grading.domain.AssessmentType.QUIZ && a.getDate() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getClassId() + "|" + a.getSubjectId() + "|" + a.getPeriodId() + "|" + a.getDate()));
        return byKey.values().stream()
                .filter(group -> group.size() >= 2)
                .map(group -> {
                    Assessment first = group.get(0);
                    return new DuplicateGroup(first.getClassId(), first.getSubjectId(), first.getPeriodId(), first.getDate(), group);
                })
                .toList();
    }

    @Override
    public Assessment mergeDuplicates(Long keepAssessmentId, Long removeAssessmentId) {
        if (keepAssessmentId.equals(removeAssessmentId)) {
            throw new BusinessRuleException("Impossible de fusionner une évaluation avec elle-même.");
        }
        Assessment keep = load(keepAssessmentId);
        Assessment remove = load(removeAssessmentId);
        if (!keep.getClassId().equals(remove.getClassId()) || !keep.getSubjectId().equals(remove.getSubjectId())
                || !keep.getPeriodId().equals(remove.getPeriodId())) {
            throw new BusinessRuleException(
                    "Ces deux évaluations ne sont pas sur la même classe/matière/période — fusion refusée.");
        }
        requireOpenPeriod(keep.getPeriodId());

        Map<Long, Grade> keepByStudent = gradeRepository.findByAssessmentId(keepAssessmentId).stream()
                .collect(java.util.stream.Collectors.toMap(Grade::getStudentId, g -> g, (x, y) -> x));
        for (Grade removeGrade : gradeRepository.findByAssessmentId(removeAssessmentId)) {
            Grade keepGrade = keepByStudent.get(removeGrade.getStudentId());
            if (keepGrade != null) {
                BigDecimal a = keepGrade.getValue();
                BigDecimal b = removeGrade.getValue();
                keepGrade.setValueA(a);
                keepGrade.setValueB(b);
                keepGrade.setValue(a.add(b).divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
                gradeRepository.save(keepGrade);
            } else {
                // Élève noté uniquement sur l'évaluation supprimée — sa note est reportée en case B,
                // jamais perdue (§retour utilisateur implicite : fusionner ne doit faire disparaître
                // aucune note déjà saisie).
                gradeRepository.save(Grade.builder()
                        .assessmentId(keepAssessmentId).studentId(removeGrade.getStudentId())
                        .value(removeGrade.getValue()).valueB(removeGrade.getValue())
                        .validated(false).enteredAt(java.time.LocalDateTime.now())
                        .build());
            }
        }
        gradeRepository.deleteByAssessmentId(removeAssessmentId);
        repo.deleteById(removeAssessmentId);
        return load(keepAssessmentId);
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
        // Libellé affichable (champ "Type" de la fiche) — jamais le nom brut de l'enum ("QUIZ",
        // "HOMEWORK"…), des valeurs internes jamais traduites (§retour utilisateur).
        String typeLabel = a.getType() == null ? null : a.getType().frenchLabel();
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

    /** Évite le doublon accidentel (double clic, nouvelle tentative après un envoi qui semblait
     * échoué...) : même classe/matière/période/type/libellé/date qu'une évaluation déjà existante
     * (§retour fondateur — des notes saisies par un professeur se sont retrouvées éclatées entre
     * plusieurs évaluations identiques, invisibles pour le principal qui n'en consultait qu'une). */
    private void requireNoDuplicate(Long classId, Long subjectId, Long periodId,
                                     gn.lacaeducation.grading.domain.AssessmentType type,
                                     String label, java.time.LocalDate date, Long excludingId) {
        if (repo.existsDuplicate(classId, subjectId, periodId, type, label, date, excludingId)) {
            throw new BusinessRuleException(
                    "Une évaluation identique existe déjà pour cette classe, cette matière et cette date "
                            + "(\"" + label + "\") — utilisez-la au lieu d'en créer une nouvelle.");
        }
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
