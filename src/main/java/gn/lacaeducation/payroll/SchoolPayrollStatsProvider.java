package gn.lacaeducation.payroll;

import java.math.BigDecimal;
import java.util.List;

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

    /**
     * Progression des heures réalisées (cahier de textes, séances marquées « Réalisée ») par rapport
     * aux heures dues (projection de l'emploi du temps sur le mois) — Vision Fondateur : le fondateur
     * doit voir combien un enseignant doit réaliser ce mois-ci et la progression au fil des séances.
     */
    record TeacherHoursProgress(Long teacherId, String teacherName, int dueMinutes, int realizedMinutes) {}

    List<TeacherHoursProgress> teacherHoursProgress(Long tenantId, int year, int month);
}
