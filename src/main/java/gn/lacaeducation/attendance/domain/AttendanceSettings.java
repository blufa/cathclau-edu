package gn.lacaeducation.attendance.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Réglage par établissement du calcul des heures enseignant réalisées : marge de transition
 * accordée entre deux séances consécutives dans des salles différentes (temps de trajet), avant de
 * compter un retard sur la seconde séance. Porte aussi l'horaire de référence du personnel
 * non-enseignant (un seul horaire pour tout le personnel, pas de créneau de cours par individu).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceSettings {
    private Long id;
    private Long tenantId;
    private Integer transitionMarginMinutes;
    private LocalTime employeeCheckInTime;
    private LocalTime employeeCheckOutTime;

    public static AttendanceSettings defaults() {
        return AttendanceSettings.builder().transitionMarginMinutes(5)
                .employeeCheckInTime(LocalTime.of(8, 0)).employeeCheckOutTime(LocalTime.of(17, 0)).build();
    }

    public int transitionMarginMinutesOrDefault() {
        return transitionMarginMinutes != null && transitionMarginMinutes >= 0 ? transitionMarginMinutes : 5;
    }

    public LocalTime employeeCheckInTimeOrDefault() {
        return employeeCheckInTime != null ? employeeCheckInTime : LocalTime.of(8, 0);
    }

    public LocalTime employeeCheckOutTimeOrDefault() {
        return employeeCheckOutTime != null ? employeeCheckOutTime : LocalTime.of(17, 0);
    }
}
