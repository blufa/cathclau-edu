package gn.lacaeducation.attendance.domain;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
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
    /** Heure du 1er pointage du jour (scan). */
    private LocalTime checkInTime;
    /** Retard cumulé du jour (somme des retards de chaque séance pointée). */
    private Integer lateMinutes;
    /** Absence justifiée ou non (null = non encore traitée). */
    private Boolean justified;
    /** URL de la pièce justificative (certificat, mot des parents…). */
    private String justificationDoc;
}
