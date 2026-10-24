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
    /** Minutes de départ anticipé (sortie pointée avant la fin prévue) — 0 si la sortie n'a jamais
     * été pointée (bénéfice du doute, pas de pénalité). */
    private Integer earlyMinutes;
    private LocalDateTime createdAt;
    /** Transitoire, jamais persisté (absent du mapping JPA) : {@code true} quand ce pointage renvoyé
     * par {@code recordEntry}/{@code recordExit} est un simple retour silencieux d'un scan déjà
     * traité (fenêtre de course réseau, sortie déjà pointée) — jamais un nouveau pointage. Permet au
     * frontend de n'afficher/vibrer qu'une seule fois par scan physique (§retour terrain : "comme si
     * il scan 8 fois"). */
    @Builder.Default
    private boolean duplicate = false;
}
