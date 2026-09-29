package gn.lacaeducation.academics.application.port.in;

import gn.lacaeducation.academics.domain.SchoolEvent;
import gn.lacaeducation.academics.domain.SchoolEventType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface GererSchoolEventUseCase {
    record CreateSchoolEventCommand(Long tenantId, String label, LocalDate startDate, LocalDate endDate,
                                    LocalTime startTime, LocalTime endTime, String reason, SchoolEventType type) {}

    /** Enseignant dont un créneau récurrent tombe dans la période/l'horaire de l'événement (« touché »),
     *  avec l'information si la direction l'a déjà crédité (payé) pour cet événement. Sans objet pour un
     *  événement HOLIDAY : tous les enseignants touchés sont de toute façon toujours payés. */
    record TouchedTeacher(Long teacherId, boolean credited) {}

    SchoolEvent create(CreateSchoolEventCommand command);
    List<SchoolEvent> findByTenant(Long tenantId);
    void delete(Long id);
    /** Enseignants dont un créneau récurrent est touché par cet événement (jour + horaire), avec leur
     *  statut de crédit actuel — pour l'écran de sélection de la direction (événements OTHER). */
    List<TouchedTeacher> findTouchedTeachers(Long schoolEventId);
    /** Remplace la liste des enseignants crédités (payés) pour cet événement — rejeté pour un HOLIDAY,
     *  qui n'a pas besoin de choix (toujours payé). */
    void setCreditedTeachers(Long schoolEventId, List<Long> teacherIds);
}
