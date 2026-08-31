package gn.lacaeducation.academics.application.port.out;

import gn.lacaeducation.academics.domain.ScheduleException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleExceptionRepository {
    ScheduleException save(ScheduleException exception);
    Optional<ScheduleException> findById(Long id);
    Optional<ScheduleException> findByOriginalScheduleIdAndWeek(Long originalScheduleId, LocalDate weekStartDate);
    /** Toutes les exceptions actives sur cette semaine (filtre Hibernate multi-tenant déjà appliqué,
     * comme {@code EmploiDuTempsRepository.findByClassId}) — le filtrage par classe/salle/enseignant se
     * fait en mémoire côté service (volumétrie hebdomadaire toujours faible). */
    List<ScheduleException> findByWeek(LocalDate weekStartDate);
    void deleteById(Long id);
    void deleteByOriginalScheduleIdAndWeek(Long originalScheduleId, LocalDate weekStartDate);
}
