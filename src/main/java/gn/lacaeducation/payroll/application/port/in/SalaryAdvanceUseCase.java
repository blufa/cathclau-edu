package gn.lacaeducation.payroll.application.port.in;

import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.SalaryAdvance;

import java.math.BigDecimal;
import java.util.List;

public interface SalaryAdvanceUseCase {

    record GrantCommand(Long tenantId, EmployeeType employeeType, Long teacherId, String teacherName,
                        BigDecimal amount, String reason) {}

    /** Résultat de la déduction automatique appliquée à la génération d'une fiche de paie —
     * {@code amount} = 0 et {@code label} = null si le salarié n'a aucune avance active. */
    record AdvanceDeductionResult(BigDecimal amount, String label) {}

    /** Accorde une avance, après vérification du plafond (salaire de référence) et qu'aucune avance
     * n'est déjà en cours pour ce salarié — {@code BusinessRuleException} sinon. */
    SalaryAdvance grant(GrantCommand command);

    List<SalaryAdvance> findByTenant(Long tenantId);

    List<SalaryAdvance> findByTeacherId(Long tenantId, Long teacherId, EmployeeType employeeType);

    /** Annule une avance non encore déduite (solde intact) — {@code BusinessRuleException} sinon. */
    SalaryAdvance cancel(Long id);

    byte[] voucherPdf(Long id);

    /**
     * Retenue automatique appliquée lors de la génération d'une fiche de paie (appelée par
     * {@code PayrollService.generate}) : déduit du solde de l'avance ACTIVE du salarié, dans la limite
     * de {@code availableNet} (le net déjà calculé sur la fiche avant cette ligne — jamais un net
     * négatif). Solde épuisé → avance passée à REPAID. Ne fait rien (montant 0) si aucune avance active.
     */
    AdvanceDeductionResult applyDeduction(Long tenantId, Long teacherId, EmployeeType employeeType, BigDecimal availableNet);
}
