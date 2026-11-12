package gn.lacaeducation.payroll.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.application.service.SalaryAdvanceService;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Masse salariale récurrente = total net du mois de paie le plus récent (§10.2). */
@Component
@RequiredArgsConstructor
class SchoolPayrollStatsProviderAdapter implements SchoolPayrollStatsProvider {

    private final PayslipRepository payslipRepository;
    private final PayrollService payrollService;
    private final EmployeePublicInfoProvider employeePublicInfoProvider;
    private final TeacherPublicInfoProvider teacherPublicInfoProvider;
    private final SalaryAdvanceService salaryAdvanceService;
    private final AcademicsDataProvider academics;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal recurringMonthlyPayroll(Long tenantId) {
        Map<YearMonth, BigDecimal> byMonth = payslipRepository.findByTenantId(tenantId).stream()
                .filter(p -> p.getPeriodStart() != null && p.getNetSalary() != null)
                .collect(Collectors.groupingBy(
                        p -> YearMonth.from(p.getPeriodStart()),
                        Collectors.reducing(BigDecimal.ZERO, Payslip::getNetSalary, BigDecimal::add)));
        return byMonth.entrySet().stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollMassSummary payrollMassSummary(Long tenantId, int year, int month) {
        PayrollService.TeacherPayrollMass teachers = payrollService.teacherPayrollMass(tenantId, year, month, validTeacherIds(tenantId));
        BigDecimal staffPayroll = activeStaffPayroll(tenantId);
        int staffCount = (int) employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active).count();
        return new PayrollMassSummary(teachers.totalHourlyRateNormal(), teachers.totalHourlyRateExam(),
                teachers.monthlyPayroll(), staffPayroll, teachers.monthlyPayroll().add(staffPayroll),
                teachers.teacherCount(), staffCount);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyFinancialSummary monthlyFinancialSummary(Long tenantId, int year, int month) {
        List<SalaryAdvance> advancesThisMonth = salaryAdvanceService.findByTenant(tenantId).stream()
                .filter(a -> a.getStatus() != SalaryAdvanceStatus.CANCELLED)
                .filter(a -> a.getGrantedAt() != null
                        && a.getGrantedAt().getYear() == year && a.getGrantedAt().getMonthValue() == month)
                .toList();
        BigDecimal advancesTotal = advancesThisMonth.stream()
                .map(SalaryAdvance::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SalaryAdvanceDetail> advanceDetails = advancesThisMonth.stream()
                .map(a -> new SalaryAdvanceDetail(a.getTeacherName(),
                        a.getEmployeeType() == EmployeeType.TEACHER ? resolveClassName(a.getTeacherId()) : null,
                        a.getGrantedAt().toLocalDate(), a.getAmount()))
                .toList();
        // §retour comptable, écart constaté en production ("je ne sais pas d'où vient cet écart") : CE
        // cumul se basait sur une masse salariale THÉORIQUE (teacherPayrollMass — heures projetées depuis
        // l'emploi du temps, salaire de base configuré pour le personnel), jamais rapprochable des sommes
        // RÉELLEMENT décaissées affichées juste à côté sur "Cumul payé pour la paie". Désormais basé,
        // comme demandé ("sur les heures réalisées, l'heure qui a été payée"), sur les MÊMES fiches de
        // paie PAID que actualPaidSalariesTotal — les deux cumuls se rapprochent enfin exactement
        // (total ci-dessous == actualPaidSalariesTotal + advancesTotal).
        ActualPaidBreakdown paid = actualPaidBreakdown(tenantId, year, month);
        BigDecimal total = paid.total().add(advancesTotal);
        return new MonthlyFinancialSummary(paid.maternelle(), paid.primary(), paid.secondary(),
                paid.staff(), advancesTotal, total, paid.total(), advanceDetails);
    }

    private record ActualPaidBreakdown(BigDecimal maternelle, BigDecimal primary, BigDecimal secondary,
                                        BigDecimal staff, BigDecimal total) {}

    /** Répartition RÉELLE (Maternelle/Primaire/Secondaire/Personnel) des fiches de paie au statut PAID
     * dont {@code paidAt} tombe dans le mois demandé — jamais une masse théorique projetée. Le cycle
     * d'un enseignant est résolu comme partout ailleurs (§{@link PayrollService#resolveTeacherCycles},
     * niveaux enseignés déclarés, pas les classes/matières assignées). */
    private ActualPaidBreakdown actualPaidBreakdown(Long tenantId, int year, int month) {
        YearMonth target = YearMonth.of(year, month);
        List<Payslip> paidThisMonth = payslipRepository.findByTenantIdAndStatus(tenantId, gn.lacaeducation.payroll.domain.PayslipStatus.PAID)
                .stream()
                .filter(p -> p.getPaidAt() != null && YearMonth.from(p.getPaidAt()).equals(target))
                .toList();
        List<Payslip> teacherPayslips = paidThisMonth.stream()
                .filter(p -> p.getEmployeeType() == EmployeeType.TEACHER)
                .toList();
        BigDecimal staff = paidThisMonth.stream()
                .filter(p -> p.getEmployeeType() != EmployeeType.TEACHER)
                .map(Payslip::getNetSalary).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Long> teacherIds = teacherPayslips.stream().map(Payslip::getTeacherId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> cycles = payrollService.resolveTeacherCycles(teacherIds);
        BigDecimal maternelle = BigDecimal.ZERO, primary = BigDecimal.ZERO, secondary = BigDecimal.ZERO;
        for (Payslip p : teacherPayslips) {
            BigDecimal net = p.getNetSalary() != null ? p.getNetSalary() : BigDecimal.ZERO;
            switch (cycles.getOrDefault(p.getTeacherId(), "SECONDAIRE")) {
                case "MATERNELLE" -> maternelle = maternelle.add(net);
                case "PRIMAIRE" -> primary = primary.add(net);
                default -> secondary = secondary.add(net);
            }
        }
        return new ActualPaidBreakdown(maternelle, primary, secondary, staff,
                maternelle.add(primary).add(secondary).add(staff));
    }

    /** Première classe trouvée pour cet enseignant (professeur principal, sinon matière assignée) —
     * juste pour affichage sur une avance sur salaire, pas une vérité exhaustive s'il en a plusieurs. */
    private String resolveClassName(Long teacherId) {
        List<ClassPublicInfo> headClasses = academics.findClassesByHeadTeacher(teacherId);
        if (!headClasses.isEmpty()) return headClasses.get(0).label();
        return academics.findClassSubjectsByTeacher(teacherId).stream()
                .findFirst()
                .flatMap(cs -> academics.findClass(cs.classId()))
                .map(ClassPublicInfo::label)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RealPayrollSummary realPayrollSummary(Long tenantId, int year, int month) {
        PayrollService.RealTeacherPayrollMass teachers = payrollService.realTeacherPayrollMass(tenantId, year, month, validTeacherIds(tenantId));
        BigDecimal staffPayroll = activeStaffPayroll(tenantId);
        return new RealPayrollSummary(teachers.monthlyPayroll(), staffPayroll,
                teachers.monthlyPayroll().add(staffPayroll), teachers.totalLateMinutes(), teachers.totalEarlyMinutes(),
                teachers.totalRealizedMinutes());
    }

    /** Enseignants actuellement existants d'un tenant — pour ignorer, dans les masses salariales
     * agrégées ci-dessus, toute fiche {@code TeacherCompensation} orpheline d'un enseignant supprimé
     * (§retour utilisateur, Vision Fondateur en production : un enseignant du primaire à salaire fixe
     * supprimé continuait de gonfler la masse salariale théorique indéfiniment). */
    private java.util.Set<Long> validTeacherIds(Long tenantId) {
        return teacherPublicInfoProvider.listTeachers(tenantId).stream()
                .map(gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo::id)
                .collect(Collectors.toSet());
    }

    private BigDecimal activeStaffPayroll(Long tenantId) {
        return employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active)
                .map(EmployeePublicInfoProvider.EmployeeInfo::baseSalary)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherHoursProgress> teacherHoursProgress(Long tenantId, int year, int month) {
        return teacherPublicInfoProvider.listTeachers(tenantId).stream()
                .map(t -> {
                    String name = ((t.firstName() != null ? t.firstName() : "") + " "
                            + (t.lastName() != null ? t.lastName() : "")).trim();
                    PayrollService.TheoreticalMonthlyPay due = payrollService.computeTheoreticalMonthlyPay(t.id(), year, month);
                    int realized = payrollService.computeMonthlyPay(t.id(), year, month).realizedMinutes();
                    return new TeacherHoursProgress(t.id(), name, due.normalMinutes() + due.examMinutes(), realized);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolYearProjection projectSchoolYear(Long tenantId, int startYear, int startMonth, int monthsCount) {
        BigDecimal staffMonthly = employeePublicInfoProvider.listEmployees(tenantId).stream()
                .filter(EmployeePublicInfoProvider.EmployeeInfo::active)
                .map(EmployeePublicInfoProvider.EmployeeInfo::baseSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MonthProjection> months = new ArrayList<>();
        BigDecimal totalTeachers = BigDecimal.ZERO;
        java.util.Set<Long> validTeacherIds = validTeacherIds(tenantId);
        YearMonth cursor = YearMonth.of(startYear, startMonth);
        for (int i = 0; i < monthsCount; i++) {
            BigDecimal teacherMonthly = payrollService.teacherPayrollMass(tenantId, cursor.getYear(), cursor.getMonthValue(), validTeacherIds).monthlyPayroll();
            totalTeachers = totalTeachers.add(teacherMonthly);
            months.add(new MonthProjection(cursor.getYear(), cursor.getMonthValue(), teacherMonthly, staffMonthly,
                    teacherMonthly.add(staffMonthly)));
            cursor = cursor.plusMonths(1);
        }
        BigDecimal totalStaff = staffMonthly.multiply(BigDecimal.valueOf(monthsCount));
        return new SchoolYearProjection(months, totalTeachers, totalStaff, totalTeachers.add(totalStaff));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSalary> teacherSalaries(Long tenantId, int year, int month, int monthsCount) {
        // Toujours construit à partir du référentiel enseignant ACTUEL (jamais seulement des fiches
        // de compensation configurées en paie) — §retour utilisateur (Vision Fondateur en production) :
        // une carte "#35" apparaissait pour un enseignant supprimé (sa fiche TeacherCompensation reste
        // orpheline en base, jamais nettoyée à la suppression), et un enseignant tout juste créé
        // n'apparaissait pas du tout tant que sa compensation n'était pas configurée. Un enseignant
        // sans compensation configurée apparaît désormais avec un salaire à 0, jamais absent ; un
        // enseignant supprimé n'apparaît plus jamais, même avec une fiche de compensation orpheline.
        Map<Long, PayrollService.TeacherSalaryBreakdown> breakdownByTeacherId = payrollService
                .teacherSalaryBreakdown(tenantId, year, month, monthsCount).stream()
                .collect(Collectors.toMap(PayrollService.TeacherSalaryBreakdown::teacherId, b -> b, (a, b) -> a));
        return teacherPublicInfoProvider.listTeachers(tenantId).stream()
                .map(t -> {
                    String name = ((t.firstName() != null ? t.firstName() : "") + " " + (t.lastName() != null ? t.lastName() : "")).trim();
                    PayrollService.TeacherSalaryBreakdown b = breakdownByTeacherId.get(t.id());
                    return new TeacherSalary(t.id(), name,
                            b != null ? b.monthlyPay() : BigDecimal.ZERO,
                            b != null ? b.annualProjection() : BigDecimal.ZERO);
                })
                .toList();
    }
}
