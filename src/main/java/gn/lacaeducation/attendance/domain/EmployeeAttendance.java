package gn.lacaeducation.attendance.domain;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Présence du personnel non-enseignant (§staff.Employee) : un seul pointage par jour, pas de créneau
 * de cours comme les enseignants — retard/départ anticipé vs les horaires configurés de l'école. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeAttendance {
    private Long id;
    private Long tenantId;
    private Long employeeId;
    private LocalDate date;
    private EmployeeAttendanceStatus status;
    private String reason;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    /** Minutes de retard vs l'heure d'entrée configurée de l'école. */
    private Integer lateMinutes;
    /** Minutes de départ anticipé vs l'heure de sortie configurée — 0 si la sortie n'a jamais été
     * pointée (bénéfice du doute, pas de pénalité). */
    private Integer earlyMinutes;
    /** Horodatage de création — utilisé pour la fenêtre de course réseau anti-double-scan (§16, voir
     * EmployeeAttendanceService#createdWithinRaceWindow), pas affiché à l'utilisateur. */
    private LocalDateTime createdAt;
    /** Transitoire, jamais persisté — voir {@code StudentCheckin#duplicate}. */
    @Builder.Default
    private boolean duplicate = false;
}
