package gn.lacaeducation.reporting.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.billing.SchoolBillingStatsProvider;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.BillingStats;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.RecoveryBreakdown;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.SubsidyStats;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.SchoolAcademicStatsProvider;
import gn.lacaeducation.grading.SchoolAcademicStatsProvider.AcademicStats;
import gn.lacaeducation.payments.SchoolCollectionStatsProvider;
import gn.lacaeducation.payments.SchoolCollectionStatsProvider.MonthlyCollection;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider.PayrollMassSummary;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.StudentDataProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Tableau de bord de l'administration de l'école (§10). Lecture seule, réservé aux rôles
 * d'administration/direction. Chaque section agrège les modules métier via leurs API publiques.
 */
@Tag(name = "School Dashboard", description = "Reporting & analytics de l'école (§10) — administration/direction")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class SchoolDashboardController {

    private final SchoolAcademicStatsProvider academicStatsProvider;
    private final SchoolBillingStatsProvider billingStatsProvider;
    private final SchoolCollectionStatsProvider collectionStatsProvider;
    private final SchoolPayrollStatsProvider payrollStatsProvider;
    private final TeacherWorkProvider teacherWorkProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;
    private final AcademicsDataProvider academicsDataProvider;
    private final StudentDataProvider studentDataProvider;

    // ── 10.1 Académique ─────────────────────────────────────────────────────
    public record ClassAcademicRow(Long classId, String className, int studentCount,
                                   BigDecimal classAverage, BigDecimal highestAverage,
                                   BigDecimal lowestAverage, BigDecimal passRate) {}
    public record TopStudentRow(Long studentId, String studentName, Long classId, String className,
                                BigDecimal overallAverage, Integer rank) {}
    public record AcademicDashboard(Long periodId, int studentCount, BigDecimal overallAverage,
                                    BigDecimal passRate, List<ClassAcademicRow> classes,
                                    List<TopStudentRow> topStudents) {}

    @Operation(summary = "Reporting académique d'une période : taux de réussite, moyennes par classe, classement global")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @GetMapping("/academic")
    public AcademicDashboard academic(@RequestParam Long periodId,
                                      @RequestParam(defaultValue = "10") int top) {
        AcademicStats stats = academicStatsProvider.academicStats(periodId, top);

        List<ClassAcademicRow> classes = stats.classStats().stream()
                .map(cs -> new ClassAcademicRow(cs.classId(), className(cs.classId()), cs.studentCount(),
                        cs.classAverage(), cs.highestAverage(), cs.lowestAverage(), cs.passRate()))
                .toList();

        List<TopStudentRow> top10 = stats.topStudents().stream()
                .map(rs -> new TopStudentRow(rs.studentId(), studentName(rs.studentId()), rs.classId(),
                        className(rs.classId()), rs.overallAverage(), rs.rank()))
                .toList();

        return new AcademicDashboard(stats.periodId(), stats.studentCount(), stats.overallAverage(),
                stats.passRate(), classes, top10);
    }

    // ── 10.2 Financier ──────────────────────────────────────────────────────
    public record FinanceDashboard(BigDecimal invoiced, BigDecimal collected, BigDecimal outstanding,
                                   BigDecimal recoveryRate, long unpaidInvoices) {}

    @Operation(summary = "Reporting financier de l'école : facturé, encaissé (recettes), impayés, taux de recouvrement")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/finance")
    public FinanceDashboard finance() {
        BillingStats s = billingStatsProvider.billingStats(TenantContext.get());
        return new FinanceDashboard(s.invoiced(), s.collected(), s.outstanding(), s.recoveryRate(), s.unpaidCount());
    }

    // ── 10.2 Financier : prévisionnel de trésorerie ─────────────────────────
    public record ProjectionMonth(int year, int month, BigDecimal expectedInflow, BigDecimal expectedOutflow,
                                  BigDecimal net, BigDecimal cumulativeNet) {}
    public record CashflowProjection(BigDecimal outstandingReceivables, BigDecimal avgMonthlyCollection,
                                     BigDecimal recurringPayroll, String collectionTrend, List<ProjectionMonth> months) {}

    @Operation(summary = "Prévisionnel de trésorerie : encaissements attendus (tendance) − masse salariale récurrente sur N mois")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','ACCOUNTANT')")
    @GetMapping("/finance/projection")
    public CashflowProjection projection(@RequestParam(defaultValue = "6") int months) {
        Long tenantId = TenantContext.get();
        int horizon = Math.max(1, Math.min(months, 24));

        // Tendance d'encaissement : moyenne des 3 derniers mois civils réalisés.
        List<MonthlyCollection> history = collectionStatsProvider.monthlyCollections(tenantId, 4);
        List<MonthlyCollection> completed = history.size() > 1 ? history.subList(0, history.size() - 1) : history;
        BigDecimal avgCollection = completed.isEmpty() ? BigDecimal.ZERO
                : completed.stream().map(MonthlyCollection::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(completed.size()), 2, java.math.RoundingMode.HALF_UP);

        // Tendance (§6, Vision Fondateur) : régression linéaire simple (pente/moyenne) sur les 6
        // derniers mois civils réalisés — aucun modèle externe, juste une lecture de la direction.
        List<MonthlyCollection> trendHistory = collectionStatsProvider.monthlyCollections(tenantId, 7);
        List<MonthlyCollection> trendCompleted = trendHistory.size() > 1
                ? trendHistory.subList(0, trendHistory.size() - 1) : trendHistory;
        String collectionTrend = computeCollectionTrend(trendCompleted);

        BigDecimal outstanding = billingStatsProvider.billingStats(tenantId).outstanding();
        BigDecimal payroll = payrollStatsProvider.recurringMonthlyPayroll(tenantId);

        // Encaissements attendus : tendance mensuelle, plafonnée par le reste dû qui se résorbe.
        java.time.YearMonth ym = java.time.YearMonth.now().plusMonths(1);
        BigDecimal remaining = outstanding;
        BigDecimal cumulative = BigDecimal.ZERO;
        List<ProjectionMonth> rows = new java.util.ArrayList<>();
        for (int i = 0; i < horizon; i++) {
            BigDecimal inflow = avgCollection.min(remaining).max(BigDecimal.ZERO);
            remaining = remaining.subtract(inflow).max(BigDecimal.ZERO);
            BigDecimal net = inflow.subtract(payroll);
            cumulative = cumulative.add(net);
            rows.add(new ProjectionMonth(ym.getYear(), ym.getMonthValue(), inflow, payroll, net, cumulative));
            ym = ym.plusMonths(1);
        }
        return new CashflowProjection(outstanding, avgCollection, payroll, collectionTrend, rows);
    }

    /**
     * Tendance des encaissements mensuels par régression linéaire simple (méthode des moindres
     * carrés sur l'indice du mois) : compare la pente à la moyenne pour classer la direction sans
     * seuil arbitraire en valeur absolue (§6 — pas de modèle externe, juste une lecture statistique).
     */
    private static String computeCollectionTrend(List<MonthlyCollection> points) {
        int n = points.size();
        if (n < 2) return "STABLE";
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = points.get(i).amount().doubleValue();
            sumX += x; sumY += y; sumXY += x * y; sumXX += x * x;
        }
        double denominator = n * sumXX - sumX * sumX;
        if (denominator == 0) return "STABLE";
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double average = sumY / n;
        if (average == 0) return "STABLE";
        double relativeSlope = slope / average;
        if (relativeSlope > 0.05) return "HAUSSE";
        if (relativeSlope < -0.05) return "BAISSE";
        return "STABLE";
    }

    // ── Vision Fondateur (features/14) ──────────────────────────────────────
    public record FounderAlert(String level, String code, String message) {}
    public record FounderYearComparison(Long compareAcademicYearId, BigDecimal invoicedDelta,
                                        BigDecimal collectedDelta, BigDecimal recoveryRateDelta) {}
    public record ClassRecoveryRow(Long classId, String className, BigDecimal invoiced, BigDecimal collected,
                                   BigDecimal outstanding, BigDecimal recoveryRate) {}
    public record TopDebtorRow(Long studentId, String studentName, Long classId, String className,
                               BigDecimal outstanding) {}
    public record RecoveryView(BillingStats overall, List<ClassRecoveryRow> byClass,
                              List<SchoolBillingStatsProvider.PeriodRecovery> byPeriod, List<TopDebtorRow> topDebtors) {}
    public record FounderDashboard(Long academicYearId, RecoveryView recovery, PayrollMassSummary payroll,
                                   SubsidyStats subsidy, List<FounderAlert> alerts,
                                   FounderYearComparison comparison) {}

    @Operation(summary = "Vision Fondateur : recouvrement détaillé, masse salariale, élèves pris en charge, "
            + "alertes automatiques et comparatif inter-années (features/14)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @GetMapping("/founder")
    public FounderDashboard founder(@RequestParam Long academicYearId,
                                    @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) Integer month,
                                    @RequestParam(required = false) Long compareAcademicYearId,
                                    @RequestParam(defaultValue = "10") int topDebtors) {
        Long tenantId = TenantContext.get();
        java.time.YearMonth now = java.time.YearMonth.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();

        RecoveryBreakdown rawRecovery = billingStatsProvider.recoveryBreakdown(tenantId, academicYearId, topDebtors);
        RecoveryView recovery = new RecoveryView(rawRecovery.overall(),
                rawRecovery.byClass().stream()
                        .map(c -> new ClassRecoveryRow(c.classId(), className(c.classId()), c.invoiced(),
                                c.collected(), c.outstanding(), c.recoveryRate()))
                        .toList(),
                rawRecovery.byPeriod(),
                rawRecovery.topDebtors().stream()
                        .map(d -> new TopDebtorRow(d.studentId(), studentName(d.studentId()), d.classId(),
                                className(d.classId()), d.outstanding()))
                        .toList());
        PayrollMassSummary payroll = payrollStatsProvider.payrollMassSummary(tenantId, y, m);
        SubsidyStats subsidy = billingStatsProvider.subsidyStats(tenantId, academicYearId);

        List<FounderAlert> alerts = new java.util.ArrayList<>();
        if (recovery.overall().recoveryRate().compareTo(BigDecimal.valueOf(60)) < 0) {
            alerts.add(new FounderAlert("WARNING", "LOW_RECOVERY_RATE",
                    "Taux de recouvrement de " + recovery.overall().recoveryRate() + " % — sous le seuil de 60 %."));
        }
        List<MonthlyCollection> history = collectionStatsProvider.monthlyCollections(tenantId, 4);
        List<MonthlyCollection> completed = history.size() > 1 ? history.subList(0, history.size() - 1) : history;
        BigDecimal avgCollection = completed.isEmpty() ? BigDecimal.ZERO
                : completed.stream().map(MonthlyCollection::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(completed.size()), 2, java.math.RoundingMode.HALF_UP);
        if (avgCollection.signum() > 0 && payroll.totalMonthlyPayroll().compareTo(avgCollection) > 0) {
            alerts.add(new FounderAlert("CRITICAL", "PAYROLL_EXCEEDS_COLLECTION",
                    "La masse salariale mensuelle (" + payroll.totalMonthlyPayroll()
                            + ") dépasse la moyenne des encaissements mensuels (" + avgCollection + ")."));
        }
        if (!recovery.topDebtors().isEmpty()) {
            BigDecimal topOutstanding = recovery.topDebtors().get(0).outstanding();
            if (recovery.overall().outstanding().signum() > 0
                    && topOutstanding.multiply(BigDecimal.valueOf(5)).compareTo(recovery.overall().outstanding()) > 0) {
                alerts.add(new FounderAlert("INFO", "DEBT_CONCENTRATION",
                        "Le plus gros débiteur représente plus de 20 % du reste dû total."));
            }
        }

        FounderYearComparison comparison = null;
        if (compareAcademicYearId != null) {
            BillingStats compareStats = billingStatsProvider
                    .recoveryBreakdown(tenantId, compareAcademicYearId, 1).overall();
            comparison = new FounderYearComparison(compareAcademicYearId,
                    recovery.overall().invoiced().subtract(compareStats.invoiced()),
                    recovery.overall().collected().subtract(compareStats.collected()),
                    recovery.overall().recoveryRate().subtract(compareStats.recoveryRate()));
        }

        return new FounderDashboard(academicYearId, recovery, payroll, subsidy, alerts, comparison);
    }

    // ── 10.3 RH ─────────────────────────────────────────────────────────────
    public record TeacherHrRow(Long teacherId, String teacherName, long presentDays, long absentDays,
                               int effectiveHours, BigDecimal attendanceRate) {}
    public record HrDashboard(int year, int month, int teacherCount, long totalPresentDays,
                              long totalAbsentDays, int totalEffectiveHours, List<TeacherHrRow> teachers) {}

    @Operation(summary = "Reporting RH d'un mois : présence et heures effectives par enseignant (§10.3)")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @GetMapping("/hr")
    public HrDashboard hr(@RequestParam int year, @RequestParam int month) {
        List<TeacherPublicInfoProvider.TeacherInfo> teachers = teacherPublicInfoProvider.listTeachers(TenantContext.get());
        List<TeacherHrRow> rows = teachers.stream()
                .map(t -> {
                    TeacherWorkProvider.MonthlyWork w = teacherWorkProvider.monthlyWork(t.id(), year, month);
                    long total = w.presentDays() + w.absentDays();
                    BigDecimal rate = total == 0 ? BigDecimal.ZERO
                            : BigDecimal.valueOf(w.presentDays()).multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);
                    String name = ((t.firstName() != null ? t.firstName() : "") + " "
                            + (t.lastName() != null ? t.lastName() : "")).trim();
                    return new TeacherHrRow(t.id(), name, w.presentDays(), w.absentDays(),
                            w.effectiveMinutes() / 60, rate);
                })
                .sorted(Comparator.comparing(TeacherHrRow::teacherName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        long present = rows.stream().mapToLong(TeacherHrRow::presentDays).sum();
        long absent = rows.stream().mapToLong(TeacherHrRow::absentDays).sum();
        int hours = rows.stream().mapToInt(TeacherHrRow::effectiveHours).sum();
        return new HrDashboard(year, month, rows.size(), present, absent, hours, rows);
    }

    // ── 10.3 RH : performance enseignant (indice composite) ─────────────────
    public record TeacherPerformanceRow(Long teacherId, String teacherName, BigDecimal performanceScore,
                                        BigDecimal completionRate, BigDecimal attendanceRate,
                                        BigDecimal punctualityRate, int effectiveHours, int lateMinutes) {}
    public record PerformanceDashboard(int year, int month, int teacherCount, List<TeacherPerformanceRow> teachers) {}

    @Operation(summary = "Performance enseignant (§10.3) : indice composite achèvement de charge / assiduité / ponctualité")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR')")
    @GetMapping("/hr/performance")
    public PerformanceDashboard performance(@RequestParam int year, @RequestParam int month) {
        List<TeacherPerformanceRow> rows = teacherPublicInfoProvider.listTeachers(TenantContext.get()).stream()
                .map(t -> {
                    TeacherWorkProvider.MonthlyWork w = teacherWorkProvider.monthlyWork(t.id(), year, month);
                    // Achèvement de la charge : minutes effectives / dues (plafonné à 100 %).
                    BigDecimal completion = ratio(w.effectiveMinutes(), w.dueMinutes());
                    // Assiduité : jours présents / jours planifiés.
                    BigDecimal attendance = ratio(w.presentDays(), w.presentDays() + w.absentDays());
                    // Ponctualité : 1 − retards / dues.
                    BigDecimal punctuality = w.dueMinutes() <= 0 ? BigDecimal.ONE
                            : BigDecimal.ONE.subtract(ratio(w.lateMinutes(), w.dueMinutes())).max(BigDecimal.ZERO);
                    // Indice composite pondéré : 50 % charge, 30 % assiduité, 20 % ponctualité.
                    BigDecimal score = completion.multiply(BigDecimal.valueOf(0.5))
                            .add(attendance.multiply(BigDecimal.valueOf(0.3)))
                            .add(punctuality.multiply(BigDecimal.valueOf(0.2)))
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    String name = ((t.firstName() != null ? t.firstName() : "") + " "
                            + (t.lastName() != null ? t.lastName() : "")).trim();
                    return new TeacherPerformanceRow(t.id(), name, score,
                            pct(completion), pct(attendance), pct(punctuality), w.effectiveMinutes() / 60, w.lateMinutes());
                })
                .sorted(Comparator.comparing(TeacherPerformanceRow::performanceScore).reversed())
                .toList();
        return new PerformanceDashboard(year, month, rows.size(), rows);
    }

    /** Ratio borné [0,1] de deux entiers (0 si dénominateur ≤ 0). */
    private static BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    private static BigDecimal pct(BigDecimal ratio) {
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String className(Long classId) {
        if (classId == null) return null;
        return academicsDataProvider.findClass(classId).map(c -> c.label()).orElse("Classe #" + classId);
    }

    private String studentName(Long studentId) {
        if (studentId == null) return null;
        return studentDataProvider.findStudent(studentId)
                .map(s -> ((s.firstName() != null ? s.firstName() : "") + " "
                        + (s.lastName() != null ? s.lastName() : "")).trim())
                .orElse("Élève #" + studentId);
    }
}
