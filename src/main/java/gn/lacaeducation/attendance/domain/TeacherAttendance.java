package gn.lacaeducation.attendance.domain;
import lombok.*;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherAttendance {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private LocalDate date;
    private TeacherAttendanceStatus status;
    private String reason;
}
