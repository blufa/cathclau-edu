package gn.lacaeducation.payroll.application.service;

import gn.lacaeducation.billing.StudentFeePaymentApi;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase.FeeDeduction;
import gn.lacaeducation.payroll.application.port.in.SalaryFeeDeductionUseCase.StaffBenefitRemainderDeduction;
import gn.lacaeducation.payroll.application.port.out.SalaryFeeDeductionRuleRepository;
import gn.lacaeducation.payroll.domain.SalaryFeeDeductionRule;
import gn.lacaeducation.staff.TeacherPublicInfoProvider;
import gn.lacaeducation.students.ParentAccessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryFeeDeductionServiceTest {

    @Mock SalaryFeeDeductionRuleRepository repo;
    @Mock TeacherPublicInfoProvider teacherProvider;
    @Mock ParentAccessProvider parentAccessProvider;
    @Mock StudentFeePaymentApi studentFeePaymentApi;
    @InjectMocks SalaryFeeDeductionService service;

    private SalaryFeeDeductionRule rule(String perChild, Integer maxChildren, String cap) {
        return SalaryFeeDeductionRule.builder().id(1L).label("Retenue frais scolaires")
                .amountPerChild(new BigDecimal(perChild)).maxChildren(maxChildren)
                .cap(cap == null ? null : new BigDecimal(cap)).active(true).build();
    }

    private void staffWithTwoChildren() {
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.of(50L));
        when(parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(50L)).thenReturn(List.of(1L, 2L));
    }

    @Test
    void computeFeeDeduction_perChildAmount_timesChildren() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, null)));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("100000");
        assertThat(d.childrenCount()).isEqualTo(2);
    }

    @Test
    void computeFeeDeduction_maxChildrenLimitsConcerned() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", 1, null)));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("50000");
        assertThat(d.childrenCount()).isEqualTo(1);
    }

    @Test
    void computeFeeDeduction_capApplies() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, "30000")));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("30000");
    }

    @Test
    void computeFeeDeduction_noChildren_returnsZero() {
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.of(50L));
        when(parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(50L)).thenReturn(List.of());

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("0");
    }

    @Test
    void computeFeeDeduction_onlyCountsChildrenWithValidatedEnrollment() {
        // L'adaptateur exclut déjà les enfants sans inscription VALIDÉE (abandon, annulée, jamais
        // inscrite) : le service ne voit que ceux qui restent, ici un seul au lieu de deux.
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.of(50L));
        when(parentAccessProvider.findChildStudentIdsWithValidatedEnrollment(50L)).thenReturn(List.of(1L));
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, null)));

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("50000");
        assertThat(d.childrenCount()).isEqualTo(1);
    }

    @Test
    void computeFeeDeduction_noRule_returnsZero() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of());

        FeeDeduction d = service.computeFeeDeduction(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("0");
    }

    // ── applyDeductionAsInvoicePayment (§payroll ↔ billing, appelé par PayrollService#markPaid) ──────

    @Test
    void applyDeductionAsInvoicePayment_appliesPerChildAmountToEachChild() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, null)));

        service.applyDeductionAsInvoicePayment(1L, 9L);

        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 1L, new BigDecimal("50000"));
        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 2L, new BigDecimal("50000"));
    }

    @Test
    void applyDeductionAsInvoicePayment_capApplies_splitsCapEquallyAcrossConcernedChildren() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", null, "60000")));

        service.applyDeductionAsInvoicePayment(1L, 9L);

        // Plafond 60000 réparti sur 2 enfants = 30000 chacun (jamais 50000, qui dépasserait ce qui a
        // réellement été retenu sur le salaire).
        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 1L, new BigDecimal("30000.00"));
        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 2L, new BigDecimal("30000.00"));
    }

    @Test
    void applyDeductionAsInvoicePayment_maxChildrenLimitsWhoGetsPayment() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of(rule("50000", 1, null)));

        service.applyDeductionAsInvoicePayment(1L, 9L);

        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 1L, new BigDecimal("50000"));
        verify(studentFeePaymentApi, never()).applyMonthlyPayment(1L, 2L, new BigDecimal("50000"));
    }

    @Test
    void applyDeductionAsInvoicePayment_noRule_doesNothing() {
        staffWithTwoChildren();
        when(repo.findByTenantId(1L)).thenReturn(List.of());

        service.applyDeductionAsInvoicePayment(1L, 9L);

        verify(studentFeePaymentApi, never()).applyMonthlyPayment(any(), any(), any());
    }

    @Test
    void applyDeductionAsInvoicePayment_notStaff_doesNothing() {
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.empty());

        service.applyDeductionAsInvoicePayment(1L, 9L);

        verify(studentFeePaymentApi, never()).applyMonthlyPayment(any(), any(), any());
    }

    // ── Reste avantage personnel (§billing ↔ payroll, avantage personnel PARTIEL) ────────────────────

    @Test
    void computeStaffBenefitRemainder_sumsAcrossChildrenWithOutstandingRemainder() {
        staffWithTwoChildren();
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(1L)).thenReturn(new BigDecimal("15000"));
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(2L)).thenReturn(new BigDecimal("25000"));

        StaffBenefitRemainderDeduction d = service.computeStaffBenefitRemainder(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("40000");
        assertThat(d.childrenCount()).isEqualTo(2);
    }

    @Test
    void computeStaffBenefitRemainder_onlyChildrenWithPositiveRemainderCounted() {
        staffWithTwoChildren();
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(1L)).thenReturn(new BigDecimal("15000"));
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(2L)).thenReturn(BigDecimal.ZERO);

        StaffBenefitRemainderDeduction d = service.computeStaffBenefitRemainder(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("15000");
        assertThat(d.childrenCount()).isEqualTo(1);
    }

    @Test
    void computeStaffBenefitRemainder_notStaff_returnsZero() {
        when(teacherProvider.findUserIdByTeacherId(9L)).thenReturn(Optional.empty());

        StaffBenefitRemainderDeduction d = service.computeStaffBenefitRemainder(1L, 9L);

        assertThat(d.amount()).isEqualByComparingTo("0");
        assertThat(d.childrenCount()).isEqualTo(0);
    }

    @Test
    void applyStaffBenefitRemainderAsInvoicePayment_appliesEachChildsRemainderSeparately() {
        staffWithTwoChildren();
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(1L)).thenReturn(new BigDecimal("15000"));
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(2L)).thenReturn(new BigDecimal("25000"));

        service.applyStaffBenefitRemainderAsInvoicePayment(1L, 9L);

        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 1L, new BigDecimal("15000"));
        verify(studentFeePaymentApi).applyMonthlyPayment(1L, 2L, new BigDecimal("25000"));
    }

    @Test
    void applyStaffBenefitRemainderAsInvoicePayment_zeroRemainder_doesNotApplyPayment() {
        staffWithTwoChildren();
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(1L)).thenReturn(BigDecimal.ZERO);
        when(studentFeePaymentApi.outstandingStaffBenefitRemainder(2L)).thenReturn(BigDecimal.ZERO);

        service.applyStaffBenefitRemainderAsInvoicePayment(1L, 9L);

        verify(studentFeePaymentApi, never()).applyMonthlyPayment(any(), any(), any());
    }
}
