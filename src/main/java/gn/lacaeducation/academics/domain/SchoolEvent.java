package gn.lacaeducation.academics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Événement de l'école (kermesse, match, entraînement...) décidé par le fondateur ou la direction, qui
 * empêche les élèves de suivre le cours sur une période donnée (ils rentrent à la maison). Les
 * enseignants dont un créneau récurrent tombe dans cette période ne sont payés sur ces heures que si la
 * direction les a explicitement crédités pour CET événement (voir
 * {@code GererSchoolEventUseCase#setCreditedTeachers}, {@code PayrollService#computeMonthlyPay}) —
 * contrairement à un jour férié (chômé et payé par l'État), qui existe séparément dans le calendrier
 * académique ({@code AcademicEventType#HOLIDAY}) et reste TOUJOURS payé automatiquement.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolEvent {
    private Long id;
    private Long tenantId;
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    /** null = couvre toute la journée (du début à la fin des horaires d'ouverture). */
    private LocalTime startTime;
    /** null = couvre toute la journée. */
    private LocalTime endTime;
    private String reason;
}
