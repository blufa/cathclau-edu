package gn.lacaeducation.attendance.application.event;

import java.time.LocalDate;

/**
 * Un élève a une présence (PRESENT/LATE) enregistrée à cette date sans avoir d'inscription active
 * déclarée dans le système — signal direct du détournement décrit par les fondateurs (élève reçu
 * et payé en main à main, jamais déclaré). §retour fondateurs, contrôle anti-fraude comptable.
 */
public record UndeclaredAttendanceEvent(Long tenantId, Long studentId, LocalDate date) {
}
