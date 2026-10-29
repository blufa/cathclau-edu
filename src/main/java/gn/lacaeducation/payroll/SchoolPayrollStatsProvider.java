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
     * Masse salariale RÉELLE (Vision Fondateur) : contrairement à {@link #payrollMassSummary}, qui
     * projette la part enseignants depuis l'emploi du temps (heures théoriques), celle-ci se base sur
     * les heures réellement réalisées (cahier de textes, retards/départs anticipés déjà déduits) —
     * avec le cumul des retards/départs anticipés de tous les enseignants, pour que le fondateur voie
     * concrètement l'écart entre ce qui est théoriquement dû et ce qui est réellement facturé, et
     * repère une anomalie de pointage. La part personnel non-enseignant reste identique (salaire de
     * base fixe, aucune notion d'heures réalisées pour eux).
     */
    record RealPayrollSummary(BigDecimal teacherMonthlyPayroll, BigDecimal staffMonthlyPayroll,
                              BigDecimal totalMonthlyPayroll, int totalLateMinutes, int totalEarlyMinutes,
                              int totalRealizedMinutes) {}

    RealPayrollSummary realPayrollSummary(Long tenantId, int year, int month);

    /**
     * Progression des heures réalisées (cahier de textes, séances marquées « Réalisée ») par rapport
     * aux heures dues (projection de l'emploi du temps sur le mois) — Vision Fondateur : le fondateur
     * doit voir combien un enseignant doit réaliser ce mois-ci et la progression au fil des séances.
     */
    record TeacherHoursProgress(Long teacherId, String teacherName, int dueMinutes, int realizedMinutes) {}

    List<TeacherHoursProgress> teacherHoursProgress(Long tenantId, int year, int month);

    /**
     * Estimation précise du salaire dû mois par mois sur l'année scolaire (par défaut 9 mois), pour
     * tout le personnel : enseignants (théorique — heures dues projetées depuis l'emploi du temps,
     * jours fériés/vacances exclus — ou salaire fixe) et personnel non-enseignant (salaire de base
     * constant). Sert à vérifier, une fois tous les emplois du temps saisis, ce que l'école devra
     * verser chaque mois de l'année.
     */
    record MonthProjection(int year, int month, BigDecimal teacherPayroll, BigDecimal staffPayroll, BigDecimal total) {}

    record SchoolYearProjection(List<MonthProjection> months, BigDecimal totalTeachers,
                                BigDecimal totalStaff, BigDecimal grandTotal) {}

    SchoolYearProjection projectSchoolYear(Long tenantId, int startYear, int startMonth, int monthsCount);

    /**
     * Salaire de CHAQUE enseignant, individuellement (pas seulement le total agrégé de
     * {@link #payrollMassSummary}) : son salaire du mois affiché et son estimation cumulée sur
     * monthsCount mois (même fenêtre que {@link #projectSchoolYear}) — Vision Fondateur.
     */
    record TeacherSalary(Long teacherId, String teacherName, BigDecimal monthlyPay, BigDecimal annualProjection) {}

    List<TeacherSalary> teacherSalaries(Long tenantId, int year, int month, int monthsCount);

    /**
     * État financier du mois (tableau de bord principal, §retour fondateur) : masse salariale des
     * enseignants répartie Maternelle/Primaire/Secondaire + personnel non-enseignant + avances sur
     * salaire ACCORDÉES ce mois (montant réellement sorti, même si déjà partiellement remboursé
     * depuis) — avec le détail de chaque avance pour affichage (nom, classe si enseignant, date,
     * montant).
     */
    record SalaryAdvanceDetail(String employeeName, String className, java.time.LocalDate grantedAt, BigDecimal amount) {}

    record MonthlyFinancialSummary(BigDecimal maternelleTeacherPayroll, BigDecimal primaryTeacherPayroll,
                                   BigDecimal secondaryTeacherPayroll, BigDecimal staffPayroll,
                                   BigDecimal salaryAdvancesTotal, BigDecimal total,
                                   List<SalaryAdvanceDetail> advances) {}

    MonthlyFinancialSummary monthlyFinancialSummary(Long tenantId, int year, int month);
}
