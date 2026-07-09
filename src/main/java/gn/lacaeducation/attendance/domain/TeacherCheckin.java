package gn.lacaeducation.attendance.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Pointage d'une séance de cours d'un enseignant : arrivée vs début du cours concerné (par séance). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherCheckin {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private LocalDate checkinDate;
    private Long scheduleSlotId;
    private LocalTime sessionStart;
    private LocalTime checkInTime;
    private Integer lateMinutes;
    private LocalDateTime createdAt;
}
