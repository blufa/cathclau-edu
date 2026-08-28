package gn.lacaeducation.attendance.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Pointage d'une séance de cours d'un élève : arrivée/sortie vs la séance concernée (par créneau),
 * miroir de {@link TeacherCheckin} côté élève. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentCheckin {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long classId;
    private LocalDate checkinDate;
    private Long scheduleSlotId;
    private LocalTime sessionStart;
    private LocalTime sessionEnd;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Integer lateMinutes;
    private LocalDateTime createdAt;
}
