package gn.lacaeducation.lessons.application.listener;

import gn.lacaeducation.grading.application.event.AssessmentScheduledEvent;
import gn.lacaeducation.lessons.application.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Quand un enseignant crée une évaluation, celle-ci est comptabilisée comme une séance
 * d'enseignement (au même titre qu'une leçon) dans ses heures travaillées : on écoute
 * {@link AssessmentScheduledEvent} et on génère l'entrée de cahier de textes correspondante.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssessmentSessionListener {

    private final LessonService lessonService;

    @TransactionalEventListener
    public void onAssessmentScheduled(AssessmentScheduledEvent event) {
        try {
            lessonService.recordAssessmentSession(event.tenantId(), event.classId(), event.subjectId(),
                    event.teacherId(), event.date(), event.label(), event.assessmentId(), event.sessionMinutes());
        } catch (Exception e) {
            log.error("Échec de l'enregistrement de la séance pour l'évaluation {}: {}",
                    event.assessmentId(), e.getMessage(), e);
        }
    }
}
