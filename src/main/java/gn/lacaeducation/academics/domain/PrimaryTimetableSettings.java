package gn.lacaeducation.academics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Grille horaire du Primaire/Maternelle configurable par tenant (§3.2 cahier des charges) : avant ce
 * réglage, `timetable-grid.tsx`/`print-timetable.ts` imposaient 08h00-18h00 par bandes fixes de 60
 * minutes pour tout cycle non-secondaire. Même convention que {@code AttendanceSettings} : champs
 * nullables + {@link #defaults()} + accesseurs {@code xxxOrDefault()}, jamais de valeur non configurée
 * appliquée silencieusement — tant que l'école n'a rien choisi, le comportement historique (08h-18h/60min)
 * reste inchangé pour ne pas perturber les emplois du temps déjà saisis.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrimaryTimetableSettings {

    public static final LocalTime DEFAULT_GRID_START = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_GRID_END = LocalTime.of(18, 0);
    public static final int DEFAULT_BAND_MINUTES = 60;

    private Long id;
    private Long tenantId;
    private LocalTime gridStartTime;
    private LocalTime gridEndTime;
    private Integer bandMinutes;

    public static PrimaryTimetableSettings defaults() {
        return PrimaryTimetableSettings.builder()
                .gridStartTime(DEFAULT_GRID_START)
                .gridEndTime(DEFAULT_GRID_END)
                .bandMinutes(DEFAULT_BAND_MINUTES)
                .build();
    }

    public LocalTime getGridStartTimeOrDefault() {
        return gridStartTime != null ? gridStartTime : DEFAULT_GRID_START;
    }

    public LocalTime getGridEndTimeOrDefault() {
        return gridEndTime != null ? gridEndTime : DEFAULT_GRID_END;
    }

    public int getBandMinutesOrDefault() {
        return bandMinutes != null ? bandMinutes : DEFAULT_BAND_MINUTES;
    }
}
