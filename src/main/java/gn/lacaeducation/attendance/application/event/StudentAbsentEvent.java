package gn.lacaeducation.attendance.application.event;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Published on every attendance outcome worth notifying the parent about — ABSENT, LATE, and (since
 * le pointage par scan de badge doit confirmer l'arrivée de l'enfant en temps réel) PRESENT on the
 * first scan of the day. Consumed by communication to notify the parents; the {@code attendanceId}
 * lets the consumer flag the record as parent-notified. {@code status} is {@code AttendanceStatus.name()}
 * ("PRESENT"/"LATE"/"ABSENT") as a plain string — not the domain enum itself, which the Modulith
 * boundary of the {@code attendance} module does not expose to other modules. {@code lateMinutes} is
 * only meaningful when {@code status.equals("LATE")} (0/null otherwise) — it is the delay of the
 * FIRST check-in of the day only, never a sum across every period/slot scanned that day.
 * {@code checkInTime} is the actual arrival time (null for a plain ABSENT with no check-in at all).
 */
public record StudentAbsentEvent(Long attendanceId, Long tenantId, Long studentId, Long classId, LocalDate date,
                                  String status, Integer lateMinutes, LocalTime checkInTime) {}
