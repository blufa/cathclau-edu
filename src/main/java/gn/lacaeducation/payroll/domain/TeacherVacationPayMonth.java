package gn.lacaeducation.payroll.domain;
import lombok.*;

/**
 * Mois précis (année + mois) où un enseignant est payé pendant les vacances/jours fériés — avantage
 * accordé par la direction au cas par cas, plutôt qu'un interrupteur valable toute l'année (§6). Un
 * mois listé ici n'est pas exclu du calcul des heures dues théoriques de ce mois précis (voir
 * PayrollService#computeTheoreticalMonthlyPay) — les autres mois de vacances restent exclus.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherVacationPayMonth {
    private Long id;
    private Long tenantId;
    private Long teacherId;
    private int year;
    private int month;
}
