package gn.lacaeducation.academics.application.port.in;

import gn.lacaeducation.academics.domain.SchoolEvent;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Gère les événements école (kermesse, sortie, entraînement...) — jamais les jours fériés, qui
 *  existent séparément dans le calendrier académique ({@code AcademicEventType.HOLIDAY}, module
 *  calendar) et restent toujours payés automatiquement, sans choix de la direction. */
public interface GererSchoolEventUseCase {
    record CreateSchoolEventCommand(Long tenantId, String label, LocalDate startDate, LocalDate endDate,
                                    LocalTime startTime, LocalTime endTime, String reason) {}

    /** Enseignant dont un créneau récurrent tombe dans la période/l'horaire de l'événement (« touché »),
     *  avec l'information si la direction l'a déjà crédité (payé) pour cet événement. */
    record TouchedTeacher(Long teacherId, boolean credited) {}

    SchoolEvent create(CreateSchoolEventCommand command);
    List<SchoolEvent> findByTenant(Long tenantId);
    void delete(Long id);
    /** Enseignants dont un créneau récurrent est touché par cet événement (jour + horaire), avec leur
     *  statut de crédit actuel — pour l'écran de sélection de la direction. */
    List<TouchedTeacher> findTouchedTeachers(Long schoolEventId);
    /** Remplace la liste des enseignants crédités (payés) pour cet événement. */
    void setCreditedTeachers(Long schoolEventId, List<Long> teacherIds);
}
