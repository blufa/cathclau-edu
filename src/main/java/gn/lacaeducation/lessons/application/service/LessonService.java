package gn.lacaeducation.lessons.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.lessons.application.port.out.LessonRepository;
import gn.lacaeducation.lessons.domain.LessonEntry;
import gn.lacaeducation.lessons.domain.LessonStatus;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cahier de textes / Lesson planning : les enseignants consignent (ou planifient) une séance par
 * classe et matière — thème, contenu, devoirs — avec un suivi PLANNED → DONE pour la progression.
 * Module autonome : référence classes/matières/enseignants uniquement par identifiant.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository repo;
    private final StudentDataProvider studentDataProvider;

    public record CreateLessonCommand(Long tenantId, Long classId, Long subjectId, Long teacherId,
                                      LocalDate lessonDate, String title, String content, String homework,
                                      Integer durationMinutes, LessonStatus status) {}

    public LessonEntry create(CreateLessonCommand c) {
        if (c.classId() == null || c.subjectId() == null) throw new BusinessRuleException("Classe et matière requises");
        if (c.title() == null || c.title().isBlank()) throw new BusinessRuleException("Le thème de la séance est requis");
        return repo.save(LessonEntry.builder()
                .tenantId(c.tenantId()).classId(c.classId()).subjectId(c.subjectId()).teacherId(c.teacherId())
                .lessonDate(c.lessonDate() != null ? c.lessonDate() : LocalDate.now())
                .title(c.title()).content(c.content()).homework(c.homework()).durationMinutes(c.durationMinutes())
                .status(c.status() != null ? c.status() : LessonStatus.PLANNED)
                .createdAt(LocalDateTime.now()).build());
    }

    /** Durée de repli d'une séance d'évaluation, si le type (donc sa durée) n'est pas renseigné (1h). */
    static final int DEFAULT_ASSESSMENT_SESSION_MINUTES = 60;

    /**
     * Enregistre une séance d'enseignement correspondant à une évaluation créée par l'enseignant :
     * une évaluation compte comme une séance de travail (comme une leçon) dans ses heures travaillées.
     * La durée dépend du type d'évaluation ({@code sessionMinutes}, fourni par le module grading) ;
     * à défaut on retombe sur {@link #DEFAULT_ASSESSMENT_SESSION_MINUTES}.
     * Idempotent : une seule séance est générée par évaluation. La séance est marquée « réalisée »
     * (DONE, comptabilisée dans les heures) dès que sa date est atteinte, sinon « planifiée ».
     */
    public LessonEntry recordAssessmentSession(Long tenantId, Long classId, Long subjectId, Long teacherId,
                                               LocalDate date, String label, Long assessmentId,
                                               Integer sessionMinutes) {
        if (assessmentId != null && repo.existsByTenantIdAndSourceAssessmentId(tenantId, assessmentId)) {
            return null; // séance déjà enregistrée pour cette évaluation
        }
        LocalDate sessionDate = date != null ? date : LocalDate.now();
        LessonStatus status = sessionDate.isAfter(LocalDate.now()) ? LessonStatus.PLANNED : LessonStatus.DONE;
        String title = "Évaluation" + (label != null && !label.isBlank() ? " : " + label : "");
        int duration = sessionMinutes != null && sessionMinutes > 0 ? sessionMinutes : DEFAULT_ASSESSMENT_SESSION_MINUTES;
        return repo.save(LessonEntry.builder()
                .tenantId(tenantId).classId(classId).subjectId(subjectId).teacherId(teacherId)
                .lessonDate(sessionDate).title(title)
                .durationMinutes(duration)
                .status(status).sourceAssessmentId(assessmentId)
                .createdAt(LocalDateTime.now()).build());
    }

    public LessonEntry update(Long id, CreateLessonCommand c) {
        LessonEntry l = load(id);
        l.setClassId(c.classId()); l.setSubjectId(c.subjectId()); l.setTeacherId(c.teacherId());
        l.setLessonDate(c.lessonDate()); l.setTitle(c.title()); l.setContent(c.content());
        l.setHomework(c.homework()); l.setDurationMinutes(c.durationMinutes());
        if (c.status() != null) l.setStatus(c.status());
        return repo.save(l);
    }

    public LessonEntry markDone(Long id) { return setStatus(id, LessonStatus.DONE); }

    public LessonEntry markPlanned(Long id) { return setStatus(id, LessonStatus.PLANNED); }

    public void delete(Long id) { repo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<LessonEntry> find(Long tenantId, Long classId, Long subjectId) {
        if (classId != null && subjectId != null) return repo.findByTenantIdAndClassIdAndSubjectId(tenantId, classId, subjectId);
        if (classId != null) return repo.findByTenantIdAndClassId(tenantId, classId);
        return repo.findByTenantId(tenantId);
    }

    /** Cahier de textes de la classe courante d'un élève — pour les portails élève et parent. */
    @Transactional(readOnly = true)
    public List<LessonEntry> findByStudent(Long studentId) {
        return studentDataProvider.findCurrentClassId(studentId)
                .map(classId -> repo.findByTenantIdAndClassId(TenantContext.get(), classId))
                .orElseGet(List::of);
    }

    @Transactional(readOnly = true)
    public LessonEntry findById(Long id) { return load(id); }

    private LessonEntry setStatus(Long id, LessonStatus s) {
        LessonEntry l = load(id);
        l.setStatus(s);
        return repo.save(l);
    }

    private LessonEntry load(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("LessonEntry", id));
    }
}
