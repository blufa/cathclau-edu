package gn.lacaeducation.grading.application.service;
import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.grading.application.port.in.AssessmentUseCase;
import gn.lacaeducation.grading.application.port.in.ExamUseCase;
import gn.lacaeducation.grading.application.port.out.AssessmentRepository;
import gn.lacaeducation.grading.application.port.out.ClassSubjectQueryPort;
import gn.lacaeducation.grading.application.port.out.DocumentStoragePort;
import gn.lacaeducation.grading.application.port.out.ExamConvocationPdfGenerator;
import gn.lacaeducation.grading.application.port.out.ExamConvocationPdfGenerator.ExamConvocationContext;
import gn.lacaeducation.grading.application.port.out.ExamRepository;
import gn.lacaeducation.grading.application.port.out.ExamSupervisionRepository;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.grading.domain.Assessment;
import gn.lacaeducation.grading.domain.AssessmentType;
import gn.lacaeducation.grading.domain.Exam;
import gn.lacaeducation.grading.domain.ExamStatus;
import gn.lacaeducation.grading.domain.ExamSupervision;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class ExamService implements ExamUseCase {
    private final ExamRepository repo;
    private final ExamSupervisionRepository supervisionRepo;
    private final ExamConvocationPdfGenerator pdfGenerator;
    private final DocumentStoragePort documentStorage;
    private final AcademicsDataProvider academicsProvider;
    private final AssessmentRepository assessmentRepository;
    private final ClassSubjectQueryPort classSubjectQueryPort;
    private final AssessmentUseCase assessmentUseCase;
    @Override public Exam create(CreateExamCommand c) {
        return repo.save(Exam.builder().tenantId(c.tenantId()).classId(c.classId()).periodId(c.periodId())
                .type(c.type()).label(c.label()).scheduledDate(c.scheduledDate()).status(ExamStatus.PLANNED).build());
    }
    @Override @Transactional(readOnly=true) public Optional<Exam> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Exam> findByClass(Long c) { return repo.findByClassId(c); }
    @Override @Transactional(readOnly=true) public List<Exam> findByPeriod(Long p) { return repo.findByPeriodId(p); }
    @Override public Exam start(Long id) { Exam e = load(id); e.setStatus(ExamStatus.ONGOING); return repo.save(e); }
    @Override public Exam complete(Long id) { Exam e = load(id); e.setStatus(ExamStatus.COMPLETED); return repo.save(e); }
    @Override @Transactional(readOnly=true) public Optional<String> getConvocation(Long id) { return Optional.ofNullable(load(id).getConvocationUrl()); }
    @Override @Transactional(readOnly=true) public byte[] convocationBytes(Long id) { Exam e = load(id); return pdfGenerator.generate(e, convocationContext(e)); }
    @Async @Override public void generateConvocationAsync(Long id) {
        // Asynchronous convocation PDF: render, store, then persist the location (idempotent).
        Exam e = load(id);
        byte[] pdf = pdfGenerator.generate(e, convocationContext(e));
        String location = documentStorage.store("exam-convocation-" + id + ".pdf", pdf);
        e.setConvocationUrl(location);
        repo.save(e);
    }
    @Override public void delete(Long id) { repo.deleteById(id); }

    // ── Surveillance ─────────────────────────────────────────────────────────────
    @Override
    public ExamSupervision assignSupervisor(Long examId, Long roomId, Long teacherId) {
        Exam exam = load(examId);
        if (roomId == null || teacherId == null) throw new BusinessRuleException("La salle et l'enseignant sont requis");
        if (supervisionRepo.existsByExamIdAndTeacherId(examId, teacherId)) {
            throw new BusinessRuleException("Cet enseignant surveille déjà cet examen");
        }
        // Conflit : le même surveillant ne peut pas être sur un autre examen le même jour.
        if (exam.getScheduledDate() != null) {
            boolean clash = supervisionRepo.findByTeacherId(teacherId).stream()
                    .filter(s -> !s.getExamId().equals(examId))
                    .map(s -> repo.findById(s.getExamId()).orElse(null))
                    .anyMatch(e -> e != null && exam.getScheduledDate().equals(e.getScheduledDate()));
            if (clash) {
                throw new BusinessRuleException("Conflit : cet enseignant surveille déjà un autre examen le "
                        + exam.getScheduledDate());
            }
        }
        return supervisionRepo.save(ExamSupervision.builder()
                .tenantId(exam.getTenantId()).examId(examId).roomId(roomId).teacherId(teacherId).build());
    }

    @Override @Transactional(readOnly = true)
    public List<ExamSupervision> supervisions(Long examId) { return supervisionRepo.findByExamId(examId); }

    @Override public void removeSupervision(Long supervisionId) { supervisionRepo.deleteById(supervisionId); }

    // ── Saisie des notes par matière (§retour utilisateur) ────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<Assessment> assessmentsByExam(Long examId) {
        return assessmentRepository.findByExamId(examId);
    }

    @Override
    public Assessment gradeEntryAssessment(Long examId, Long subjectId) {
        Exam exam = load(examId);
        Optional<Assessment> existing = assessmentRepository.findByExamId(examId).stream()
                .filter(a -> subjectId.equals(a.getSubjectId()))
                .findFirst();
        if (existing.isPresent()) return existing.get();

        if (exam.getScheduledDate() == null) {
            throw new BusinessRuleException("Renseignez une date pour cette composition avant de saisir les notes");
        }
        // Coefficient/barème neutres (jamais utilisés par SubjectSemesterAverage pour une composition,
        // qui applique le coefficient de la MATIÈRE — voir sa Javadoc) : juste des valeurs valides pour
        // les colonnes NOT NULL d'assessments.
        Long teacherId = classSubjectQueryPort.findByClassId(exam.getClassId()).stream()
                .filter(i -> subjectId.equals(i.subjectId()))
                .map(ClassSubjectQueryPort.ClassSubjectInfo::teacherId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Aucun enseignant affecté à cette matière pour cette classe"));
        return assessmentUseCase.create(new AssessmentUseCase.CreateAssessmentCommand(
                exam.getTenantId(), exam.getClassId(), subjectId, teacherId, exam.getPeriodId(),
                AssessmentType.EXAM, null, exam.getLabel(), exam.getScheduledDate(),
                BigDecimal.ONE, new BigDecimal("20"), examId));
    }

    private Exam load(Long id) { return repo.findById(id).orElseThrow(() -> new gn.lacaeducation.common.ResourceNotFoundException("Exam", id)); }

    /** Resolves the class name, period label and school year so the convocation shows names, not IDs. */
    private ExamConvocationContext convocationContext(Exam e) {
        String className = academicsProvider.findClass(e.getClassId())
                .map(c -> c.levelLabel() != null && !c.levelLabel().isBlank() ? c.label() + " (" + c.levelLabel() + ")" : c.label())
                .orElse(null);
        String periodLabel = academicsProvider.findPeriod(e.getPeriodId()).map(p -> p.label()).orElse(null);
        String schoolYear = e.getTenantId() == null ? null : academicsProvider.currentAcademicYearLabel(e.getTenantId()).orElse(null);
        return new ExamConvocationContext(className, periodLabel, schoolYear);
    }
}
