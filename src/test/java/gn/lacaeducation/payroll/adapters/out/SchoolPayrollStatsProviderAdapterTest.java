package gn.lacaeducation.payroll.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.academics.ClassPublicInfo;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider.MonthlyFinancialSummary;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider.PayrollMassSummary;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider.TeacherSalary;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.application.service.PayrollService.TeacherSalaryBreakdown;
import gn.lacaeducation.payroll.application.service.SalaryAdvanceService;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.PayslipStatus;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.staff.TeacherPublicInfoProvider.TeacherInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * §retour utilisateur (Vision Fondateur en production) : "Salaire par enseignant" affichait une
 * carte "#35" pour un enseignant supprimé, et un enseignant tout juste créé n'apparaissait pas du
 * tout — les deux venaient du fait que la liste était construite depuis les fiches
 * {@code TeacherCompensation} (paie configurée) plutôt que depuis le référentiel enseignant actuel.
 */
@ExtendWith(MockitoExtension.class)
class SchoolPayrollStatsProviderAdapterTest {

    @Mock PayrollService payrollService;
    @Mock EmployeePublicInfoProvider employeePublicInfoProvider;
    @Mock TeacherPublicInfoProvider teacherPublicInfoProvider;
    @Mock SalaryAdvanceService salaryAdvanceService;
    @Mock AcademicsDataProvider academics;
    @Mock PayslipRepository payslipRepository;
    @InjectMocks SchoolPayrollStatsProviderAdapter adapter;

    @Test
    void teacherSalaries_deletedTeacherWithOrphanedCompensation_neverAppears() {
        // Compensation orpheline pour un enseignant supprimé (id 35) — plus dans le référentiel.
        when(payrollService.teacherSalaryBreakdown(10L, 2026, 11, 9)).thenReturn(List.of(
                new TeacherSalaryBreakdown(35L, new BigDecimal("500000"), new BigDecimal("4500000"))));
        when(teacherPublicInfoProvider.listTeachers(10L)).thenReturn(List.of());

        List<TeacherSalary> result = adapter.teacherSalaries(10L, 2026, 11, 9);

        assertThat(result).isEmpty();
    }

    @Test
    void teacherSalaries_newTeacherWithoutCompensation_appearsWithZero() {
        when(payrollService.teacherSalaryBreakdown(10L, 2026, 11, 9)).thenReturn(List.of());
        when(teacherPublicInfoProvider.listTeachers(10L)).thenReturn(List.of(
                new TeacherInfo(42L, "Fatoumata", "Bah", null, null, null, null, null, null, null)));

        List<TeacherSalary> result = adapter.teacherSalaries(10L, 2026, 11, 9);

        assertThat(result).hasSize(1);
        TeacherSalary t = result.get(0);
        assertThat(t.teacherId()).isEqualTo(42L);
        assertThat(t.teacherName()).isEqualTo("Fatoumata Bah");
        assertThat(t.monthlyPay()).isEqualByComparingTo("0");
        assertThat(t.annualProjection()).isEqualByComparingTo("0");
    }

    @Test
    void teacherSalaries_existingTeacherWithCompensation_showsBreakdownValues() {
        when(payrollService.teacherSalaryBreakdown(10L, 2026, 11, 9)).thenReturn(List.of(
                new TeacherSalaryBreakdown(7L, new BigDecimal("800000"), new BigDecimal("7200000"))));
        when(teacherPublicInfoProvider.listTeachers(10L)).thenReturn(List.of(
                new TeacherInfo(7L, "Mamadou", "Diallo", null, null, null, null, null, null, null)));

        List<TeacherSalary> result = adapter.teacherSalaries(10L, 2026, 11, 9);

        assertThat(result).hasSize(1);
        TeacherSalary t = result.get(0);
        assertThat(t.teacherName()).isEqualTo("Mamadou Diallo");
        assertThat(t.monthlyPay()).isEqualByComparingTo("800000");
        assertThat(t.annualProjection()).isEqualByComparingTo("7200000");
    }

    @Test
    void payrollMassSummary_passesCurrentTeacherIdsToIgnoreOrphanedCompensation() {
        when(teacherPublicInfoProvider.listTeachers(10L)).thenReturn(List.of(
                new TeacherInfo(2L, "Mamadou", "Diallo", null, null, null, null, null, null, null)));
        when(payrollService.teacherPayrollMass(eq(10L), eq(2026), eq(3), any()))
                .thenReturn(new PayrollService.TeacherPayrollMass(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("900000"), 1,
                        BigDecimal.ZERO, new BigDecimal("900000"), BigDecimal.ZERO));
        when(employeePublicInfoProvider.listEmployees(10L)).thenReturn(List.of());

        PayrollMassSummary summary = adapter.payrollMassSummary(10L, 2026, 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        org.mockito.Mockito.verify(payrollService).teacherPayrollMass(eq(10L), eq(2026), eq(3), captor.capture());
        assertThat(captor.getValue()).containsExactly(2L);
        assertThat(summary.teacherCount()).isEqualTo(1);
        assertThat(summary.teacherMonthlyPayroll()).isEqualByComparingTo("900000");
    }

    // §retour comptable, écart constaté en production ("je ne sais pas d'où vient cet écart") : la
    // répartition Maternelle/Primaire/Secondaire/Personnel se base désormais sur les fiches PAID
    // réellement décaissées ce mois (même source que actualPaidSalariesTotal), jamais sur la masse
    // théorique projetée (teacherPayrollMass/activeStaffPayroll) — les deux cumuls se rapprochent enfin.

    @Test
    void monthlyFinancialSummary_basesBreakdownOnActualPaidPayslipsByTeacherCycle() {
        Payslip maternelleTeacher = Payslip.builder().employeeType(EmployeeType.TEACHER).teacherId(1L)
                .netSalary(new BigDecimal("1000000")).paidAt(LocalDateTime.of(2026, 10, 20, 10, 0)).build();
        Payslip secondaryTeacher = Payslip.builder().employeeType(EmployeeType.TEACHER).teacherId(2L)
                .netSalary(new BigDecimal("1200000")).paidAt(LocalDateTime.of(2026, 10, 21, 10, 0)).build();
        Payslip staffPayslip = Payslip.builder().employeeType(EmployeeType.ADMINISTRATIVE).teacherId(9L)
                .netSalary(new BigDecimal("300000")).paidAt(LocalDateTime.of(2026, 10, 22, 10, 0)).build();
        // Payée le mois précédent — ne doit JAMAIS compter dans l'état financier d'octobre.
        Payslip paidLastMonth = Payslip.builder().employeeType(EmployeeType.TEACHER).teacherId(1L)
                .netSalary(new BigDecimal("400000")).paidAt(LocalDateTime.of(2026, 9, 20, 10, 0)).build();
        when(payslipRepository.findByTenantIdAndStatus(10L, PayslipStatus.PAID)).thenReturn(List.of(
                maternelleTeacher, secondaryTeacher, staffPayslip, paidLastMonth));
        when(payrollService.resolveTeacherCycles(List.of(1L, 2L)))
                .thenReturn(java.util.Map.of(1L, "MATERNELLE", 2L, "SECONDAIRE"));
        SalaryAdvance inMonth = SalaryAdvance.builder().teacherId(5L).teacherName("Mamadou Diallo")
                .employeeType(EmployeeType.TEACHER).amount(new BigDecimal("300000"))
                .status(SalaryAdvanceStatus.ACTIVE).grantedAt(LocalDateTime.of(2026, 10, 15, 9, 0)).build();
        SalaryAdvance otherMonth = SalaryAdvance.builder().teacherId(6L).teacherName("Fama Camara")
                .employeeType(EmployeeType.TEACHER).amount(new BigDecimal("200000"))
                .status(SalaryAdvanceStatus.ACTIVE).grantedAt(LocalDateTime.of(2026, 9, 15, 9, 0)).build();
        SalaryAdvance cancelledInMonth = SalaryAdvance.builder().teacherId(7L).teacherName("Oumar Bah")
                .employeeType(EmployeeType.TEACHER).amount(new BigDecimal("100000"))
                .status(SalaryAdvanceStatus.CANCELLED).grantedAt(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
        when(salaryAdvanceService.findByTenant(10L)).thenReturn(List.of(inMonth, otherMonth, cancelledInMonth));
        when(academics.findClassesByHeadTeacher(5L)).thenReturn(List.of(
                new ClassPublicInfo(1L, "CM2", "CM2", "NORMALE", null, 5L, "PRIMAIRE")));

        MonthlyFinancialSummary summary = adapter.monthlyFinancialSummary(10L, 2026, 10);

        assertThat(summary.maternelleTeacherPayroll()).isEqualByComparingTo("1000000");
        assertThat(summary.secondaryTeacherPayroll()).isEqualByComparingTo("1200000");
        assertThat(summary.primaryTeacherPayroll()).isEqualByComparingTo("0");
        assertThat(summary.staffPayroll()).isEqualByComparingTo("300000");
        assertThat(summary.salaryAdvancesTotal()).isEqualByComparingTo("300000");
        // 1 000 000 + 1 200 000 + 300 000 (fiches payées en octobre) + 300 000 (avance d'octobre).
        assertThat(summary.total()).isEqualByComparingTo("2800000");
        // Même rapprochement que "Cumul payé pour la paie" (frontend) : actualPaidSalariesTotal +
        // salaryAdvancesTotal == total ci-dessus.
        assertThat(summary.actualPaidSalariesTotal()).isEqualByComparingTo("2500000");
        assertThat(summary.advances()).hasSize(1);
        assertThat(summary.advances().get(0).employeeName()).isEqualTo("Mamadou Diallo");
        assertThat(summary.advances().get(0).className()).isEqualTo("CM2");
    }

    @Test
    void monthlyFinancialSummary_nonTeacherAdvance_noClassName() {
        when(payslipRepository.findByTenantIdAndStatus(10L, PayslipStatus.PAID)).thenReturn(List.of());
        SalaryAdvance staffAdvance = SalaryAdvance.builder().teacherId(8L).teacherName("Aissatou Sow")
                .employeeType(EmployeeType.ADMINISTRATIVE).amount(new BigDecimal("150000"))
                .status(SalaryAdvanceStatus.ACTIVE).grantedAt(LocalDateTime.of(2026, 10, 5, 9, 0)).build();
        when(salaryAdvanceService.findByTenant(10L)).thenReturn(List.of(staffAdvance));

        MonthlyFinancialSummary summary = adapter.monthlyFinancialSummary(10L, 2026, 10);

        assertThat(summary.advances()).hasSize(1);
        assertThat(summary.advances().get(0).className()).isNull();
    }
}
