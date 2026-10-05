package gn.lacaeducation.attendance.application.event;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Publié au scan de sortie (§retour fondateurs) — {@code checkInTime} est celle du PREMIER pointage
 * du jour (pas forcément celui du créneau en cours, si l'élève a été pointé entrée/sortie sur
 * plusieurs créneaux dans la journée), pour que {@code minutesAtSchool} reflète la durée totale
 * passée à l'école ce jour-là, cohérente avec son emploi du temps (6h, 8h...), pas seulement le
 * dernier créneau pointé.
 */
public record StudentExitEvent(Long tenantId, Long studentId, Long classId, LocalDate date,
                                LocalTime checkInTime, LocalTime checkOutTime, int minutesAtSchool) {
}
