package gn.lacaeducation.reporting.adapters.in.web;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.attendance.TeacherWorkProvider;
import gn.lacaeducation.billing.SchoolBillingStatsProvider;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.BillingStats;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.grading.SchoolAcademicStatsProvider;
import gn.lacaeducation.grading.SchoolAcademicStatsProvider.AcademicStats;
import gn.lacaeducation.payments.SchoolCollectionStatsProvider;
import gn.lacaeducation.payments.SchoolCollectionStatsProvider.MonthlyCollection;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider;
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
                                     BigDecimal recurringPayroll, List<ProjectionMonth> months) {}

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
        return new CashflowProjection(outstanding, avgCollection, payroll, rows);
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
