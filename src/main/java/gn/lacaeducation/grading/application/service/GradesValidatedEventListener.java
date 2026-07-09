package gn.lacaeducation.grading.application.service;

import gn.lacaeducation.grading.application.event.GradesValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens to {@link GradesValidatedEvent} (NotesValidees) and recomputes the report cards
 * (subject averages, general average, ranks and class statistics) for the assessment's
 * class + period. Validating grades therefore immediately "applies" them: the computed
 * averages reflect the newly locked grades without waiting for the period to be closed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GradesValidatedEventListener {

    private final GradeCalculationService calculationService;

    @Async
    @EventListener
    public void onGradesValidated(GradesValidatedEvent event) {
        if (event.classId() == null || event.periodId() == null) {
            log.debug("GradesValidatedEvent without class/period (assessmentId={}); skipping recompute.",
                    event.assessmentId());
            return;
        }
        try {
            log.info("Recomputing report cards after grade validation: classId={}, periodId={}",
                    event.classId(), event.periodId());
            calculationService.generateForClass(event.classId(), event.periodId());
        } catch (Exception e) {
            log.error("Failed to recompute report cards for classId={}, periodId={}: {}",
                    event.classId(), event.periodId(), e.getMessage(), e);
        }
    }
}
