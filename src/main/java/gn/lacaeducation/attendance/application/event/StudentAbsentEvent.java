package gn.lacaeducation.attendance.application.event;
import java.time.LocalDate;
public record StudentAbsentEvent(Long tenantId, Long studentId, Long classId, LocalDate date) {}
