package gn.lacaeducation.payroll.domain;

import lombok.*;

import java.math.BigDecimal;

/**
 * Paramètres de rémunération d'un enseignant / membre du personnel. Deux tarifs horaires (§6) :
 * un pour les classes normales, un pour les classes d'examen — appliqués selon la typologie de
 * chaque classe où l'enseignant intervient (voir PayrollService#computeTheoreticalMonthlyPay).
 * Le paiement pendant les vacances (avantage accordé par la direction, §6) se gère mois par mois
 * précis, voir {@link TeacherVacationPayMonth}.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherCompensation {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private PayMode payMode;
    private BigDecimal hourlyRateNormal;
    private BigDecimal hourlyRateExam;
    private BigDecimal fixedSalary;
}
