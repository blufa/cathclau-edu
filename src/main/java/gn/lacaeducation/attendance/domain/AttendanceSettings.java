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
 * compter un retard sur la seconde séance — s'applique aux enseignants ET au personnel non-enseignant
 * (même horaire de référence pour tout le personnel). Les élèves ont leur propre marge distincte
 * ({@link #studentTransitionMarginMinutes}), réglable indépendamment : le temps de trajet d'un
 * enseignant entre deux salles n'a pas de raison de suivre la même tolérance que celle accordée à un
 * élève qui arrive en classe. Porte aussi l'horaire de référence du personnel non-enseignant (un
 * seul horaire pour tout le personnel, pas de créneau de cours par individu).
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceSettings {
    private Long id;
    private Long tenantId;
    private Integer transitionMarginMinutes;
    private Integer studentTransitionMarginMinutes;
    private LocalTime employeeCheckInTime;
    private LocalTime employeeCheckOutTime;

    public static AttendanceSettings defaults() {
        return AttendanceSettings.builder().transitionMarginMinutes(0).studentTransitionMarginMinutes(0)
                .employeeCheckInTime(LocalTime.of(8, 0)).employeeCheckOutTime(LocalTime.of(17, 0)).build();
    }

    // Pas de tolérance tant que l'école ne l'a pas explicitement configurée (§réglages) — une valeur
    // par défaut non nulle appliquait silencieusement une marge que la direction n'avait jamais choisie.
    // Enseignants + personnel — voir studentTransitionMarginMinutesOrDefault() pour les élèves.
    public int transitionMarginMinutesOrDefault() {
        return transitionMarginMinutes != null && transitionMarginMinutes >= 0 ? transitionMarginMinutes : 0;
    }

    public int studentTransitionMarginMinutesOrDefault() {
        return studentTransitionMarginMinutes != null && studentTransitionMarginMinutes >= 0
                ? studentTransitionMarginMinutes : 0;
    }

    public LocalTime employeeCheckInTimeOrDefault() {
        return employeeCheckInTime != null ? employeeCheckInTime : LocalTime.of(8, 0);
    }

    public LocalTime employeeCheckOutTimeOrDefault() {
        return employeeCheckOutTime != null ? employeeCheckOutTime : LocalTime.of(17, 0);
    }
}
