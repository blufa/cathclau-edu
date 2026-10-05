package gn.lacaeducation.attendance.application.event;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Publié pour un enseignant en retard (premier pointage du jour uniquement, jamais redéclenché par
 * les scans suivants de la même journée) ou absent (marquage automatique 22h ou manuel) — §retour
 * fondateurs. Contrairement à {@code StudentAbsentEvent}, jamais publié pour un statut PRESENT à
 * l'heure (pas d'intérêt à notifier une arrivée normale). Porte directement {@code teacherName}/
 * {@code teacherPhone} (résolus ici, dans {@code attendance}) plutôt que de faire dépendre
 * {@code communication} du module {@code staff} pour ce seul besoin — même principe que
 * {@code PayslipPaidEvent} (module payroll).
 */
public record TeacherAbsentEvent(Long tenantId, Long teacherId, String teacherName, String teacherPhone,
                                  LocalDate date, String status, Integer lateMinutes, LocalTime checkInTime) {
}
