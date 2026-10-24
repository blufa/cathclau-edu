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
    private LocalTime sessionEnd;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Integer lateMinutes;
    /** sessionEnd − checkOutTime quand la sortie est pointée avant la fin prévue (0 si sortie non
     * pointée : bénéfice du doute, pas de pénalité de départ anticipé). */
    private Integer earlyMinutes;
    private LocalDateTime createdAt;
    /** Transitoire, jamais persisté — voir {@code StudentCheckin#duplicate}. */
    @Builder.Default
    private boolean duplicate = false;
}
