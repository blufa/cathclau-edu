package gn.lacaeducation.lessons.application.service;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassSubjectPublicInfo;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.lessons.application.event.LessonValidatedWithoutCheckinEvent;
import gn.lacaeducation.lessons.application.port.out.LessonRepository;
import gn.lacaeducation.lessons.domain.LessonEntry;
import gn.lacaeducation.lessons.domain.LessonStatus;
import gn.lacaeducation.students.StudentDataProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    private final AcademicsDataProvider academics;
    private final ApplicationEventPublisher eventPublisher;

    public record CreateLessonCommand(Long tenantId, Long classId, Long subjectId, Long teacherId,
                                      LocalDate lessonDate, String title, String content, String homework,
                                      Integer durationMinutes, LessonStatus status) {}

    public LessonEntry create(CreateLessonCommand c) {
        if (c.classId() == null) throw new BusinessRuleException("Classe requise");
        if (c.title() == null || c.title().isBlank()) throw new BusinessRuleException("Le thème de la séance est requis");
        // Matière optionnelle : un enseignant unique couvrant toute la classe (moniteur maternelle/
        // primaire, salaire fixe) consigne sa journée sans choisir de matière précise — l'enseignant se
        // résout alors via le professeur principal de la classe plutôt que via l'affectation matière.
        Long teacherId = c.subjectId() != null
                ? resolveTeacherId(c.classId(), c.subjectId())
                : resolveHeadTeacherId(c.classId());
        return repo.save(LessonEntry.builder()
                .tenantId(c.tenantId()).classId(c.classId()).subjectId(c.subjectId()).teacherId(teacherId)
                .lessonDate(c.lessonDate() != null ? c.lessonDate() : LocalDate.now())
                .title(c.title()).content(c.content()).homework(c.homework()).durationMinutes(c.durationMinutes())
                .status(c.status() != null ? c.status() : LessonStatus.PLANNED)
                .createdAt(LocalDateTime.now()).build());
    }

    /**
     * Résout systématiquement le professeur depuis l'affectation classe↔matière (ClasseMatiere),
     * source de vérité — la paie (heures réalisées) se base sur {@code LessonEntry.teacherId} : une
     * séance sans professeur affecté ne doit jamais être créée silencieusement (§ retour utilisateur).
     */
    private Long resolveTeacherId(Long classId, Long subjectId) {
        return academics.findClassSubjects(classId).stream()
                .filter(cs -> subjectId.equals(cs.subjectId()))
                .map(ClassSubjectPublicInfo::teacherId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Aucun enseignant n'est affecté à cette matière pour cette classe. "
                                + "Affectez un enseignant (Référentiels → Classes → Matières) avant de programmer une séance."));
    }

    /** Résout l'enseignant d'une séance sans matière précise via le professeur principal de la classe
     * (cas du moniteur maternelle/primaire, seul enseignant, salaire fixe). */
    private Long resolveHeadTeacherId(Long classId) {
        return academics.findClass(classId)
                .map(gn.lacaeducation.academics.ClassPublicInfo::headTeacherId)
                .filter(Objects::nonNull)
                .orElseThrow(() -> new BusinessRuleException(
                        "Aucun professeur principal désigné pour cette classe. Renseignez-le (fiche classe) "
                                + "ou choisissez une matière pour que le système résolve l'enseignant."));
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
        Long teacherId = c.subjectId() != null
                ? resolveTeacherId(c.classId(), c.subjectId())
                : resolveHeadTeacherId(c.classId());
        l.setClassId(c.classId()); l.setSubjectId(c.subjectId()); l.setTeacherId(teacherId);
        l.setLessonDate(c.lessonDate()); l.setTitle(c.title()); l.setContent(c.content());
        l.setHomework(c.homework()); l.setDurationMinutes(c.durationMinutes());
        if (c.status() != null) l.setStatus(c.status());
        return repo.save(l);
    }

    public LessonEntry markDone(Long id) { return markDone(id, null, null); }

    public LessonEntry markDone(Long id, Integer realizedDurationMinutes) {
        return markDone(id, realizedDurationMinutes, null);
    }

    /**
     * Marque la séance réalisée, en corrigeant éventuellement sa durée avec le temps réellement
     * effectué (ex. un professeur en retard rattrape les heures perdues) — c'est cette durée réalisée,
     * jamais la durée planifiée, qui alimente ensuite la masse salariale (§payroll).
     *
     * @param confirmedWithoutCheckin la personne qui valide a explicitement reconnu l'absence de
     *                                pointage enseignant correspondant (§retour fondateurs, contrôle
     *                                anti-fraude — voir MarkDoneForm côté frontend) ; déclenche une
     *                                trace d'audit dédiée, jamais un blocage (un override légitime —
     *                                scanner en panne, admin présent en personne — doit rester possible).
     */
    public LessonEntry markDone(Long id, Integer realizedDurationMinutes, Boolean confirmedWithoutCheckin) {
        LessonEntry l = load(id);
        if (realizedDurationMinutes != null) {
            if (realizedDurationMinutes < 0) throw new BusinessRuleException("La durée réalisée ne peut pas être négative");
            l.setDurationMinutes(realizedDurationMinutes);
        }
        l.setStatus(LessonStatus.DONE);
        LessonEntry saved = repo.save(l);
        if (Boolean.TRUE.equals(confirmedWithoutCheckin)) {
            eventPublisher.publishEvent(new LessonValidatedWithoutCheckinEvent(
                    saved.getTenantId(), saved.getId(), saved.getTeacherId(), saved.getLessonDate()));
        }
        return saved;
    }

    public LessonEntry markPlanned(Long id) { return setStatus(id, LessonStatus.PLANNED); }

    public void delete(Long id) { repo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<LessonEntry> find(Long tenantId, Long classId, Long subjectId, Long teacherId) {
        List<LessonEntry> base;
        if (classId != null && subjectId != null) base = repo.findByTenantIdAndClassIdAndSubjectId(tenantId, classId, subjectId);
        else if (classId != null) base = repo.findByTenantIdAndClassId(tenantId, classId);
        else base = repo.findByTenantId(tenantId);
        if (subjectId != null && classId == null) base = base.stream().filter(l -> subjectId.equals(l.getSubjectId())).toList();
        if (teacherId != null) base = base.stream().filter(l -> teacherId.equals(l.getTeacherId())).toList();
        return base;
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
