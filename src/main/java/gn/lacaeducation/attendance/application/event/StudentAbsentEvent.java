package gn.lacaeducation.attendance.application.event;
import java.time.LocalDate;

/**
 * Published when a student is recorded ABSENT or LATE. Consumed by communication to notify the
 * parents; the {@code attendanceId} lets the consumer flag the record as parent-notified.
 */
public record StudentAbsentEvent(Long attendanceId, Long tenantId, Long studentId, Long classId, LocalDate date) {}
