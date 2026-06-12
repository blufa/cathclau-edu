package gn.lacaeducation.academics.application.listener;

import gn.lacaeducation.academics.application.port.out.ClasseRepository;
import gn.lacaeducation.academics.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.students.application.event.EnrollmentCancelledEvent;
import gn.lacaeducation.students.application.event.EnrollmentValidatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps a class headcount in sync with validated enrollments (§5.4): on every validated enrollment
 * the class effectif is recomputed from the authoritative validated-enrollment count and stored.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventListener {

    private final ClasseRepository classeRepository;
    private final EnrollmentQueryPort enrollmentQueryPort;

    @TransactionalEventListener
    @Transactional
    public void onEnrollmentValidated(EnrollmentValidatedEvent event) {
        recomputeHeadcount(event.classId());
    }

    @TransactionalEventListener
    @Transactional
    public void onEnrollmentCancelled(EnrollmentCancelledEvent event) {
        recomputeHeadcount(event.classId());
    }

    private void recomputeHeadcount(Long classId) {
        classeRepository.findById(classId).ifPresent(classe -> {
            long count = enrollmentQueryPort.countValidatedByClassId(classId);
            classe.setHeadcount((int) count);
            classeRepository.save(classe);
            log.debug("Recomputed headcount={} for class {}", count, classId);
        });
    }
}
