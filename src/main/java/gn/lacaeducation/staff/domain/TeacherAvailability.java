package gn.lacaeducation.staff.domain;

import lombok.*;

import java.time.LocalTime;

/** Plage de disponibilité d'un enseignant un jour donné (ex. Lundi 08:00–12:00). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherAvailability {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private String day;          // LUNDI..SAMEDI
    private LocalTime startTime;
    private LocalTime endTime;
}
