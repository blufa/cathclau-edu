package gn.lacaeducation.attendance.domain;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherAttendance {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private LocalDate date;
    private TeacherAttendanceStatus status;
    private String reason;
    /** Heure d'arrivée (pointage). */
    private LocalTime checkInTime;
    /** Minutes de retard vs début du premier cours du jour. */
    private Integer lateMinutes;
}
