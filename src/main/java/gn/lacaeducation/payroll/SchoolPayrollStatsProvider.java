package gn.lacaeducation.payroll;

import java.math.BigDecimal;

/**
 * API publique du module payroll pour le reporting financier (§10.2) : masse salariale récurrente
 * d'un tenant, utilisée comme estimation des sorties dans le prévisionnel de trésorerie.
 */
public interface SchoolPayrollStatsProvider {
    /** Total net des fiches de paie du mois le plus récent (0 si aucune paie). */
    BigDecimal recurringMonthlyPayroll(Long tenantId);

    /** Masse salariale complète (enseignants + personnel) d'un tenant pour un mois (Vision Fondateur §10.2). */
    record PayrollMassSummary(BigDecimal totalHourlyRateNormal, BigDecimal totalHourlyRateExam,
                              BigDecimal teacherMonthlyPayroll, BigDecimal staffMonthlyPayroll,
                              BigDecimal totalMonthlyPayroll, int teacherCount, int staffCount) {}

    PayrollMassSummary payrollMassSummary(Long tenantId, int year, int month);
}
