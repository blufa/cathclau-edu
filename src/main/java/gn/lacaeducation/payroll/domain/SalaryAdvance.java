package gn.lacaeducation.payroll.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Avance sur salaire (§payroll) : montant accordé à un enseignant/personnel, plafonné à son salaire de
 * référence (net de la dernière fiche de paie, à défaut son salaire de base/fixe configuré), remboursée
 * automatiquement par retenue sur les fiches de paie suivantes jusqu'à épuisement de
 * {@link #remainingBalance} — jamais une règle recalculée, un solde qui s'épuise (voir
 * {@code SalaryAdvanceService#applyDeduction}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAdvance {
    private Long id;
    private Long tenantId;
    /** Nature du salarié, comme sur {@link Payslip} — {@code teacherId} référence deux référentiels
     * distincts (Teacher ou Employee) dont les identifiants peuvent coïncider. */
    private EmployeeType employeeType;
    private Long teacherId;
    private String teacherName;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private String reason;
    private SalaryAdvanceStatus status;
    private LocalDateTime grantedAt;
    private LocalDateTime lastDeductedAt;
}
