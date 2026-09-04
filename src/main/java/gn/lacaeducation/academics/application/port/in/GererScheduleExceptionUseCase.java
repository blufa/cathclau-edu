package gn.lacaeducation.academics.application.port.in;

import gn.lacaeducation.academics.domain.ScheduleException;
import gn.lacaeducation.academics.domain.valueobject.JourSemaine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Déplacement/annulation ponctuelle d'un créneau récurrent ({@link gn.lacaeducation.academics.domain.EmploiDuTemps})
 * pour UNE semaine précise, sans jamais modifier le créneau récurrent lui-même (§5.6).
 */
public interface GererScheduleExceptionUseCase {

    /** Déplace un créneau récurrent pour une semaine précise. Idempotent : rejouer pour la même
     * semaine met à jour la cible au lieu de dupliquer l'exception. */
    ScheduleException moveForWeek(MoveScheduleForWeekCommand command);

    /** Annule un créneau récurrent pour une semaine précise (le cours n'a pas lieu, sans déplacement). */
    ScheduleException cancelForWeek(Long originalScheduleId, LocalDate weekStartDate, String reason);

    /** Supprime l'exception : le créneau redevient récurrent (position d'origine) pour cette semaine. */
    void cancelException(Long originalScheduleId, LocalDate weekStartDate);

    /** Emploi du temps effectif d'une classe pour une semaine donnée (récurrent + déplacements/annulations). */
    List<EffectiveScheduleSlot> effectiveScheduleForClass(Long classId, LocalDate weekStartDate);

    /** Idem pour un enseignant (inclut les créneaux d'autres classes déplacés vers lui cette semaine). */
    List<EffectiveScheduleSlot> effectiveScheduleForTeacher(Long teacherId, LocalDate weekStartDate);

    record MoveScheduleForWeekCommand(
            Long tenantId, Long originalScheduleId, LocalDate weekStartDate,
            JourSemaine newDay, LocalTime newStartTime, LocalTime newEndTime,
            Long newRoomId, Long newTeacherId, String reason) {}

    /** Projection de lecture consolidée — un slot par occurrence réelle de la semaine. */
    record EffectiveScheduleSlot(
            Long scheduleId, Long schoolClassId, Long subjectId, Long teacherId, Long roomId,
            JourSemaine day, LocalTime startTime, LocalTime endTime,
            boolean moved, boolean cancelled,
            JourSemaine originalDay, LocalTime originalStartTime, LocalTime originalEndTime) {}
}
