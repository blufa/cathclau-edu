package gn.lacaeducation.attendance.domain;
import lombok.*;
import java.time.LocalDate;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentAttendance {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private LocalDate date;
    private AttendanceStatus status;
    private String reason;
    private Boolean parentNotified;
    /** Absence justifiée ou non (null = non encore traitée). */
    private Boolean justified;
    /** URL de la pièce justificative (certificat, mot des parents…). */
    private String justificationDoc;
}
