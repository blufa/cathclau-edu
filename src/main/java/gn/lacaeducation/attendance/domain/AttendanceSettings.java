package gn.lacaeducation.attendance.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Réglage par établissement du calcul des heures enseignant réalisées : marge de transition
 * accordée entre deux séances consécutives dans des salles différentes (temps de trajet), avant de
 * compter un retard sur la seconde séance.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceSettings {
    private Long id;
    private Long tenantId;
    private Integer transitionMarginMinutes;

    public static AttendanceSettings defaults() {
        return AttendanceSettings.builder().transitionMarginMinutes(5).build();
    }

    public int transitionMarginMinutesOrDefault() {
        return transitionMarginMinutes != null && transitionMarginMinutes >= 0 ? transitionMarginMinutes : 5;
    }
}
