package gn.lacaeducation.attendance.application.event;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Publié au scan de sortie d'un enseignant (§retour fondateurs) — même mécanique que
 * {@link StudentExitEvent} (durée depuis le premier pointage du jour, pas seulement le créneau en
 * cours), mais SANS notification automatique : un enseignant n'a pas de parent à prévenir. Laissé
 * disponible pour un futur consommateur (ex. paie à l'heure), aucun écouteur pour l'instant.
 */
public record TeacherExitEvent(Long tenantId, Long teacherId, LocalDate date,
                                LocalTime checkInTime, LocalTime checkOutTime, int minutesAtSchool) {
}
