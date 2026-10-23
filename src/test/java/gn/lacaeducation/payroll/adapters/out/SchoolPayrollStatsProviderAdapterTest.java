package gn.lacaeducation.payroll.adapters.out;

import gn.lacaeducation.payroll.SchoolPayrollStatsProvider.PayrollMassSummary;
import gn.lacaeducation.payroll.SchoolPayrollStatsProvider.TeacherSalary;
import gn.lacaeducation.payroll.application.service.PayrollService;
import gn.lacaeducation.payroll.application.service.PayrollService.TeacherSalaryBreakdown;
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
                new TeacherInfo(42L, "Fatoumata", "Bah", null, null, null, null, null)));

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
                new TeacherInfo(7L, "Mamadou", "Diallo", null, null, null, null, null)));

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
                new TeacherInfo(2L, "Mamadou", "Diallo", null, null, null, null, null)));
        when(payrollService.teacherPayrollMass(eq(10L), eq(2026), eq(3), any()))
                .thenReturn(new PayrollService.TeacherPayrollMass(BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("900000"), 1));
        when(employeePublicInfoProvider.listEmployees(10L)).thenReturn(List.of());

        PayrollMassSummary summary = adapter.payrollMassSummary(10L, 2026, 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        org.mockito.Mockito.verify(payrollService).teacherPayrollMass(eq(10L), eq(2026), eq(3), captor.capture());
        assertThat(captor.getValue()).containsExactly(2L);
        assertThat(summary.teacherCount()).isEqualTo(1);
        assertThat(summary.teacherMonthlyPayroll()).isEqualByComparingTo("900000");
    }
}
