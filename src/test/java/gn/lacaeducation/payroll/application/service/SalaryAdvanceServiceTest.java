package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase.AdvanceDeductionResult;
import gn.lacaeducation.payroll.application.port.in.SalaryAdvanceUseCase.GrantCommand;
import gn.lacaeducation.payroll.application.port.out.PayslipRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryAdvanceRepository;
import gn.lacaeducation.payroll.application.port.out.SalaryAdvanceVoucherPdfGenerator;
import gn.lacaeducation.payroll.application.port.out.TeacherCompensationRepository;
import gn.lacaeducation.payroll.domain.EmployeeType;
import gn.lacaeducation.payroll.domain.PayMode;
import gn.lacaeducation.payroll.domain.Payslip;
import gn.lacaeducation.payroll.domain.SalaryAdvance;
import gn.lacaeducation.payroll.domain.SalaryAdvanceStatus;
import gn.lacaeducation.payroll.domain.TeacherCompensation;
import gn.lacaeducation.staff.EmployeePublicInfoProvider;
import gn.lacaeducation.staff.EmployeePublicInfoProvider.EmployeeInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryAdvanceServiceTest {

    @Mock SalaryAdvanceRepository repo;
    @Mock PayslipRepository payslipRepo;
    @Mock TeacherCompensationRepository compRepo;
    @Mock EmployeePublicInfoProvider employeeProvider;
    @Mock SalaryAdvanceVoucherPdfGenerator pdfGenerator;
    @InjectMocks SalaryAdvanceService service;

    private SalaryAdvance advance(BigDecimal amount, BigDecimal remaining, SalaryAdvanceStatus status) {
        return SalaryAdvance.builder().id(1L).tenantId(1L).employeeType(EmployeeType.TEACHER).teacherId(9L)
                .teacherName("Mamadou Diallo").amount(amount).remainingBalance(remaining).status(status)
                .grantedAt(LocalDateTime.now()).build();
    }

    // ── grant() : plafond ────────────────────────────────────────────────────

    @Test
    void grant_withinLastPayslipNet_succeeds() {
        lenient().when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of());
        when(payslipRepo.findByTenantIdAndTeacherId(1L, 9L)).thenReturn(List.of(
                Payslip.builder().employeeType(EmployeeType.TEACHER).netSalary(new BigDecimal("500000"))
                        .createdAt(LocalDateTime.now()).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SalaryAdvance a = service.grant(new GrantCommand(1L, EmployeeType.TEACHER, 9L, "Mamadou Diallo",
                new BigDecimal("200000"), "Urgence médicale"));

        assertThat(a.getStatus()).isEqualTo(SalaryAdvanceStatus.ACTIVE);
        assertThat(a.getRemainingBalance()).isEqualByComparingTo("200000");
    }

    @Test
    void grant_exceedsLastPayslipNet_throws() {
        lenient().when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of());
        when(payslipRepo.findByTenantIdAndTeacherId(1L, 9L)).thenReturn(List.of(
                Payslip.builder().employeeType(EmployeeType.TEACHER).netSalary(new BigDecimal("200000"))
                        .createdAt(LocalDateTime.now()).build()));

        assertThatThrownBy(() -> service.grant(new GrantCommand(1L, EmployeeType.TEACHER, 9L, "Mamadou Diallo",
                new BigDecimal("500000"), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void grant_noPayslip_fallsBackToFixedTeacherCompensation() {
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of());
        when(payslipRepo.findByTenantIdAndTeacherId(1L, 9L)).thenReturn(List.of());
        when(compRepo.findByTeacherId(9L)).thenReturn(Optional.of(
                TeacherCompensation.builder().teacherId(9L).payMode(PayMode.FIXED).fixedSalary(new BigDecimal("300000")).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SalaryAdvance a = service.grant(new GrantCommand(1L, EmployeeType.TEACHER, 9L, "Mamadou Diallo",
                new BigDecimal("250000"), null));

        assertThat(a.getRemainingBalance()).isEqualByComparingTo("250000");
    }

    @Test
    void grant_employeeType_fallsBackToEmployeeBaseSalary() {
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 12L, EmployeeType.ADMINISTRATIVE)).thenReturn(List.of());
        when(payslipRepo.findByTenantIdAndTeacherId(1L, 12L)).thenReturn(List.of());
        when(employeeProvider.findEmployee(12L)).thenReturn(Optional.of(
                new EmployeeInfo(12L, "Aissatou Bah", "ADMINISTRATIVE", new BigDecimal("400000"), true, null, null)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SalaryAdvance a = service.grant(new GrantCommand(1L, EmployeeType.ADMINISTRATIVE, 12L, "Aissatou Bah",
                new BigDecimal("100000"), null));

        assertThat(a.getRemainingBalance()).isEqualByComparingTo("100000");
    }

    @Test
    void grant_noReferenceAvailable_throws() {
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of());
        when(payslipRepo.findByTenantIdAndTeacherId(1L, 9L)).thenReturn(List.of());
        when(compRepo.findByTeacherId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grant(new GrantCommand(1L, EmployeeType.TEACHER, 9L, "Mamadou Diallo",
                new BigDecimal("100000"), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void grant_alreadyHasActiveAdvance_throws() {
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER))
                .thenReturn(List.of(advance(new BigDecimal("100000"), new BigDecimal("50000"), SalaryAdvanceStatus.ACTIVE)));

        assertThatThrownBy(() -> service.grant(new GrantCommand(1L, EmployeeType.TEACHER, 9L, "Mamadou Diallo",
                new BigDecimal("50000"), null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(payslipRepo, never()).findByTenantIdAndTeacherId(any(), any());
    }

    // ── cancel() ─────────────────────────────────────────────────────────────

    @Test
    void cancel_balanceIntact_succeeds() {
        SalaryAdvance a = advance(new BigDecimal("100000"), new BigDecimal("100000"), SalaryAdvanceStatus.ACTIVE);
        when(repo.findById(1L)).thenReturn(Optional.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SalaryAdvance cancelled = service.cancel(1L);

        assertThat(cancelled.getStatus()).isEqualTo(SalaryAdvanceStatus.CANCELLED);
    }

    @Test
    void cancel_alreadyPartiallyDeducted_throws() {
        SalaryAdvance a = advance(new BigDecimal("100000"), new BigDecimal("60000"), SalaryAdvanceStatus.ACTIVE);
        when(repo.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(BusinessRuleException.class);
        verify(repo, never()).save(any());
    }

    // ── applyDeduction() ─────────────────────────────────────────────────────

    @Test
    void applyDeduction_noActiveAdvance_returnsZero() {
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of());

        AdvanceDeductionResult r = service.applyDeduction(1L, 9L, EmployeeType.TEACHER, new BigDecimal("500000"));

        assertThat(r.amount()).isEqualByComparingTo("0");
        verify(repo, never()).save(any());
    }

    @Test
    void applyDeduction_fullyCoveredByAvailableNet_repaysInOneShot() {
        SalaryAdvance a = advance(new BigDecimal("200000"), new BigDecimal("200000"), SalaryAdvanceStatus.ACTIVE);
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        AdvanceDeductionResult r = service.applyDeduction(1L, 9L, EmployeeType.TEACHER, new BigDecimal("500000"));

        assertThat(r.amount()).isEqualByComparingTo("200000");
        ArgumentCaptor<SalaryAdvance> captor = ArgumentCaptor.forClass(SalaryAdvance.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRemainingBalance()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getStatus()).isEqualTo(SalaryAdvanceStatus.REPAID);
    }

    @Test
    void applyDeduction_availableNetSmallerThanBalance_deductsPartialAndStaysActive() {
        SalaryAdvance a = advance(new BigDecimal("200000"), new BigDecimal("200000"), SalaryAdvanceStatus.ACTIVE);
        when(repo.findByTenantIdAndTeacherIdAndEmployeeType(1L, 9L, EmployeeType.TEACHER)).thenReturn(List.of(a));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        AdvanceDeductionResult r = service.applyDeduction(1L, 9L, EmployeeType.TEACHER, new BigDecimal("80000"));

        assertThat(r.amount()).isEqualByComparingTo("80000");
        ArgumentCaptor<SalaryAdvance> captor = ArgumentCaptor.forClass(SalaryAdvance.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRemainingBalance()).isEqualByComparingTo("120000");
        assertThat(captor.getValue().getStatus()).isEqualTo(SalaryAdvanceStatus.ACTIVE);
    }

    @Test
    void applyDeduction_availableNetZeroOrNegative_returnsZero() {
        AdvanceDeductionResult r = service.applyDeduction(1L, 9L, EmployeeType.TEACHER, BigDecimal.ZERO);

        assertThat(r.amount()).isEqualByComparingTo("0");
        verify(repo, never()).findByTenantIdAndTeacherIdAndEmployeeType(any(), any(), any());
    }
}
