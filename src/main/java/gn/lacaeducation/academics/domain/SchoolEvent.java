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
 * enseignants dont un créneau récurrent tombe dans cette période restent payés sur ces heures comme si
 * le cours avait eu lieu — jamais retirées de leurs heures réalisées (voir
 * {@code PayrollService#computeMonthlyPay}).
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
