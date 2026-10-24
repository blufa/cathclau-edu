package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.grading.application.event.GradesValidatedEvent;
import gn.lacaeducation.grading.application.port.in.GradeUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.GradeHistoryRepository;
import gn.lacaeducation.grading.application.port.out.GradeRepository;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.Grade;
import gn.lacaeducation.grading.domain.GradeAction;
import gn.lacaeducation.grading.domain.GradeHistory;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
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
    private final GradeHistoryRepository historyRepository;
    private final StudentDataProvider studentDataProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final GradeCalculationService calculationService;
    private final gn.lacaeducation.grading.application.port.out.SubjectGradeReportPdfGenerator subjectReportPdfGenerator;

    @Override
    public Grade enter(EnterGradeCommand c, Long changedBy) {
        Assessment assessment = requireOpenPeriod(c.assessmentId());
        validateValue(c.value(), assessment);
        Grade saved = repo.save(Grade.builder()
                .assessmentId(c.assessmentId())
                .studentId(c.studentId())
                .value(c.value())
                .validated(false)
                .enteredAt(LocalDateTime.now())
                .build());
        recordHistory(saved, GradeAction.ENTERED, null, saved.getValue(), changedBy, null);
        return saved;
    }

    @Override
    public List<Grade> enterBulk(Long assessmentId, List<BulkGradeEntry> entries, boolean allowOverwrite, Long changedBy) {
        Assessment assessment = requireOpenPeriod(assessmentId);
        entries.forEach(e -> validateValue(e.value(), assessment));
        // Upsert by (assessment, student). An already-entered grade can only be changed by an
        // authorized role (allowOverwrite); a teacher cannot modify a grade once it is entered.
        java.util.Map<Long, Grade> existing = repo.findByAssessmentId(assessmentId).stream()
                .collect(java.util.stream.Collectors.toMap(Grade::getStudentId, g -> g, (a, b) -> a));
        List<Grade> toSave = new java.util.ArrayList<>();
        // Track the previous value + action per row so history can be recorded after the batch save.
        java.util.Map<Long, BigDecimal> oldValueByStudent = new java.util.HashMap<>();
        java.util.Map<Long, GradeAction> actionByStudent = new java.util.HashMap<>();
        for (BulkGradeEntry e : entries) {
            Grade g = existing.get(e.studentId());
            if (g != null) {
                if (Boolean.TRUE.equals(g.getValidated())) continue;        // validated → locked
                if (!allowOverwrite) continue;                              // teacher: cannot modify an entered grade
                oldValueByStudent.put(e.studentId(), g.getValue());
                actionByStudent.put(e.studentId(), GradeAction.UPDATED);
                g.setValue(e.value());
                g.setEnteredAt(LocalDateTime.now());
                toSave.add(g);
            } else {
                actionByStudent.put(e.studentId(), GradeAction.ENTERED);
                toSave.add(Grade.builder()
                        .assessmentId(assessmentId)
                        .studentId(e.studentId())
                        .value(e.value())
                        .validated(false)
                        .enteredAt(LocalDateTime.now())
                        .build());
            }
        }
        List<Grade> saved = repo.saveAll(toSave);
        for (Grade g : saved) {
            GradeAction action = actionByStudent.getOrDefault(g.getStudentId(), GradeAction.ENTERED);
            recordHistory(g, action, oldValueByStudent.get(g.getStudentId()), g.getValue(), changedBy, null);
        }
        return saved;
    }

    @Override
    public List<Grade> validateAllForAssessment(Long assessmentId, Long changedBy) {
        List<Grade> grades = repo.findByAssessmentId(assessmentId);
        List<Grade> saved = grades.stream()
                .filter(g -> !Boolean.TRUE.equals(g.getValidated()))
                .peek(g -> g.setValidated(true))
                .toList();
        List<Grade> persisted = repo.saveAll(saved);
        persisted.forEach(g -> recordHistory(g, GradeAction.VALIDATED, g.getValue(), g.getValue(), changedBy, null));
        publishValidated(assessmentId);
        return persisted;
    }

    @Override
    public Grade update(Long id, BigDecimal value, Long changedBy, String reason) {
        Grade g = load(id);
        if (Boolean.TRUE.equals(g.getValidated())) {
            throw new BusinessRuleException("Grade " + id + " is validated (locked); unlock it before correcting.");
        }
        // La modification d'une note exige un motif : il est tracé et communiqué à l'administration.
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Un motif est obligatoire pour modifier une note.");
        }
        Assessment assessment = requireOpenPeriod(g.getAssessmentId());
        validateValue(value, assessment);
        BigDecimal oldValue = g.getValue();
        g.setValue(value);
        Grade saved = repo.save(g);
        recordHistory(saved, GradeAction.UPDATED, oldValue, value, changedBy, reason);
        // Alerte l'ADMIN_ECOLE et le responsable pédagogique de la modification (motif inclus).
        eventPublisher.publishEvent(new gn.lacaeducation.grading.application.event.GradeModifiedEvent(
                assessment.getTenantId(), saved.getId(), saved.getAssessmentId(), saved.getStudentId(),
                oldValue, value, changedBy, reason));
        return saved;
    }

    @Override
    public Grade validate(Long id, Long changedBy) {
        Grade g = load(id);
        g.setValidated(true);
        Grade saved = repo.save(g);
        recordHistory(saved, GradeAction.VALIDATED, saved.getValue(), saved.getValue(), changedBy, null);
        publishValidated(g.getAssessmentId());
        return saved;
    }

    @Override
    public Grade unlock(Long id, Long changedBy) {
        Grade g = load(id);
        g.setValidated(false);
        Grade saved = repo.save(g);
        recordHistory(saved, GradeAction.UNLOCKED, saved.getValue(), saved.getValue(), changedBy, null);
        return saved;
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
    public List<GradeSheetRow> gradeSheet(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment", assessmentId));
        java.util.Map<Long, Grade> gradeByStudent = repo.findByAssessmentId(assessmentId).stream()
                .collect(java.util.stream.Collectors.toMap(Grade::getStudentId, g -> g, (a, b) -> a));
        return studentDataProvider.findStudentsByClass(assessment.getClassId()).stream()
                .map(s -> {
                    Grade g = gradeByStudent.get(s.id());
                    return new GradeSheetRow(s.id(), s.studentNumber(), fullName(s),
                            g != null ? g.getId() : null,
                            g != null ? g.getValue() : null,
                            g != null ? g.getValidated() : null);
                })
                .toList();
    }

    private static String fullName(StudentPublicInfo s) {
        String first = s.firstName() != null ? s.firstName() : "";
        String last = s.lastName() != null ? s.lastName() : "";
        return (first + " " + last).trim();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectGradeReport subjectGradeReport(Long classId, Long subjectId, Long periodId) {
        return buildSubjectGradeReport(classId, subjectId, periodId, null);
    }

    /**
     * @param month si non nul (1-12), limite le relevé aux évaluations datées de ce mois-là — utilisé
     *              pour le « relevé du mois » (deux évaluations orale/écrite du même mois par exemple),
     *              par opposition au relevé complet de la période.
     */
    private SubjectGradeReport buildSubjectGradeReport(Long classId, Long subjectId, Long periodId, Integer month) {
        // Évaluations de la matière pour la classe/période, ordonnées par date puis identifiant.
        List<Assessment> assessments = assessmentRepository.findByClassIdAndPeriodId(classId, periodId).stream()
                .filter(a -> subjectId.equals(a.getSubjectId()))
                .filter(a -> month == null || (a.getDate() != null && a.getDate().getMonthValue() == month))
                .sorted(java.util.Comparator
                        .comparing((Assessment a) -> a.getDate() == null ? java.time.LocalDate.MIN : a.getDate())
                        .thenComparing(Assessment::getId))
                .toList();
        // Jamais mélangées (§retour utilisateur) : "cours" (interrogations/devoirs) d'un côté,
        // compositions de l'autre — même distinction que SubjectSemesterAverage. FINAL (examen de fin
        // de période) hors des deux, déjà hors barème semestriel standard (voir sa Javadoc).
        List<Assessment> coursAssessments = assessments.stream()
                .filter(a -> a.getType() == gn.lacaeducation.grading.domain.AssessmentType.QUIZ
                        || a.getType() == gn.lacaeducation.grading.domain.AssessmentType.HOMEWORK)
                .toList();
        List<Assessment> compoAssessments = assessments.stream()
                .filter(a -> a.getType() == gn.lacaeducation.grading.domain.AssessmentType.EXAM)
                .toList();

        // Notes par évaluation : assessmentId → (studentId → note).
        java.util.Map<Long, java.util.Map<Long, Grade>> gradesByAssessment = new java.util.HashMap<>();
        for (Assessment a : assessments) {
            gradesByAssessment.put(a.getId(), repo.findByAssessmentId(a.getId()).stream()
                    .collect(java.util.stream.Collectors.toMap(Grade::getStudentId, g -> g, (x, y) -> x)));
        }

        List<ReleveRow> coursRows = new java.util.ArrayList<>();
        List<ReleveRow> compoRows = new java.util.ArrayList<>();
        for (StudentPublicInfo s : studentDataProvider.findStudentsByClass(classId)) {
            java.util.Map<Long, BigDecimal> allNotes = new java.util.LinkedHashMap<>();
            List<Grade> studentGrades = new java.util.ArrayList<>();
            for (Assessment a : assessments) {
                Grade g = gradesByAssessment.get(a.getId()).get(s.id());
                allNotes.put(a.getId(), g == null ? null : g.getValue());
                if (g != null && g.getValue() != null) studentGrades.add(g);
            }
            // Calculé UNE fois sur la liste combinée (comme avant) : SubjectSemesterAverage trie
            // lui-même cours/composition en interne — coursValue/compoValue exposent ce détail déjà
            // "coefcié" (voir sa Javadoc), aucun recalcul ici.
            gn.lacaeducation.grading.domain.SubjectSemesterAverage.Result detail =
                    calculationService.subjectAverageDetail(assessments, studentGrades);
            coursRows.add(new ReleveRow(s.id(), s.studentNumber(), fullName(s),
                    notesFor(coursAssessments, allNotes), detail == null ? null : detail.coursValue()));
            compoRows.add(new ReleveRow(s.id(), s.studentNumber(), fullName(s),
                    notesFor(compoAssessments, allNotes), detail == null ? null : detail.compoValue()));
        }
        coursRows.sort(java.util.Comparator.comparing(ReleveRow::fullName, String.CASE_INSENSITIVE_ORDER));
        compoRows.sort(java.util.Comparator.comparing(ReleveRow::fullName, String.CASE_INSENSITIVE_ORDER));

        Long tenantId = assessments.stream().map(Assessment::getTenantId)
                .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        String className = academicsDataProvider.findClass(classId)
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String subjectName = academicsDataProvider.subjectLabels(java.util.Set.of(subjectId)).get(subjectId);
        String periodLabel = academicsDataProvider.findPeriod(periodId).map(p -> p.label()).orElse(null);
        String schoolYear = tenantId == null ? null : academicsDataProvider.currentAcademicYearLabel(tenantId).orElse(null);
        BigDecimal subjectCoefficient = calculationService.subjectCoefficient(classId, subjectId);

        return new SubjectGradeReport(tenantId, classId, subjectId, periodId,
                className, subjectName, periodLabel, schoolYear, subjectCoefficient,
                new ReleveSection(toReleveAssessments(coursAssessments), coursRows),
                new ReleveSection(toReleveAssessments(compoAssessments), compoRows));
    }

    private static List<ReleveAssessment> toReleveAssessments(List<Assessment> assessments) {
        return assessments.stream()
                .map(a -> new ReleveAssessment(a.getId(), a.getLabel(),
                        a.getType() == null ? null : a.getType().name(), a.getDate(), a.getMaxScore(), a.getCoefficient()))
                .toList();
    }

    private static java.util.Map<Long, BigDecimal> notesFor(List<Assessment> section, java.util.Map<Long, BigDecimal> allNotes) {
        java.util.Map<Long, BigDecimal> notes = new java.util.LinkedHashMap<>();
        for (Assessment a : section) notes.put(a.getId(), allNotes.get(a.getId()));
        return notes;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] subjectGradeReportPdf(Long classId, Long subjectId, Long periodId, Integer month) {
        return subjectReportPdfGenerator.generate(buildSubjectGradeReport(classId, subjectId, periodId, month));
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
    @Transactional(readOnly = true)
    public List<GradeHistory> findHistory(Long gradeId) {
        return historyRepository.findByGradeId(gradeId);
    }

    @Override
    public void delete(Long id, Long changedBy) {
        Grade g = load(id);
        recordHistory(g, GradeAction.DELETED, g.getValue(), null, changedBy, null);
        repo.deleteById(id);
    }

    /** Publishes {@link GradesValidatedEvent} enriched with the assessment's class + period for recompute. */
    private void publishValidated(Long assessmentId) {
        Assessment a = assessmentRepository.findById(assessmentId).orElse(null);
        Long classId = a != null ? a.getClassId() : null;
        Long periodId = a != null ? a.getPeriodId() : null;
        eventPublisher.publishEvent(new GradesValidatedEvent(assessmentId, classId, periodId));
    }

    private void recordHistory(Grade grade, GradeAction action, BigDecimal oldValue, BigDecimal newValue,
                               Long changedBy, String reason) {
        historyRepository.save(GradeHistory.builder()
                .gradeId(grade.getId())
                .assessmentId(grade.getAssessmentId())
                .studentId(grade.getStudentId())
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .reason(reason)
                .changedAt(LocalDateTime.now())
                .build());
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
