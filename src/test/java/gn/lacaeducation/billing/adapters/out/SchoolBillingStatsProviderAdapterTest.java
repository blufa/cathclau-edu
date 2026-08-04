package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.SchoolBillingStatsProvider.BillingStats;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.ClassRecovery;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.PeriodRecovery;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.RecoveryBreakdown;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.SubsidyStats;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.TopDebtor;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolBillingStatsProviderAdapterTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock ExemptionRepository exemptionRepository;
    @InjectMocks SchoolBillingStatsProviderAdapter adapter;

    private Invoice inv(String total, String paid, String remaining, InvoiceStatus status) {
        return Invoice.builder().totalAmount(new BigDecimal(total)).paidAmount(new BigDecimal(paid))
                .remainingBalance(new BigDecimal(remaining)).status(status).build();
    }

    @Test
    void billingStats_sumsAndComputesRecoveryRate() {
        when(invoiceRepository.findByTenantId(9L)).thenReturn(List.of(
                inv("100000", "100000", "0", InvoiceStatus.PAID),
                inv("100000", "40000", "60000", InvoiceStatus.PARTIAL),
                inv("50000", "0", "50000", InvoiceStatus.UNPAID)));

        BillingStats s = adapter.billingStats(9L);

        assertThat(s.invoiced()).isEqualByComparingTo("250000");
        assertThat(s.collected()).isEqualByComparingTo("140000");
        assertThat(s.outstanding()).isEqualByComparingTo("110000");
        // 140000 / 250000 = 56.00 %
        assertThat(s.recoveryRate()).isEqualByComparingTo("56.00");
        assertThat(s.unpaidCount()).isEqualTo(2); // PARTIAL + UNPAID
    }

    @Test
    void billingStats_noInvoices_isZero() {
        when(invoiceRepository.findByTenantId(9L)).thenReturn(List.of());
        BillingStats s = adapter.billingStats(9L);
        assertThat(s.invoiced()).isEqualByComparingTo("0");
        assertThat(s.recoveryRate()).isEqualByComparingTo("0");
        assertThat(s.unpaidCount()).isZero();
    }

    @Test
    void recoveryBreakdown_groupsByClassAndPeriodAndRanksTopDebtors() {
        Invoice classA1 = Invoice.builder().studentId(1L).feeId(100L).periodLabel("Octobre 2025")
                .totalAmount(new BigDecimal("100000")).paidAmount(new BigDecimal("100000"))
                .remainingBalance(BigDecimal.ZERO).status(InvoiceStatus.PAID).build();
        Invoice classA2 = Invoice.builder().studentId(2L).feeId(100L).periodLabel("Novembre 2025")
                .totalAmount(new BigDecimal("100000")).paidAmount(new BigDecimal("20000"))
                .remainingBalance(new BigDecimal("80000")).status(InvoiceStatus.PARTIAL).build();
        Invoice classB1 = Invoice.builder().studentId(3L).feeId(200L).periodLabel("Octobre 2025")
                .totalAmount(new BigDecimal("50000")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("50000")).status(InvoiceStatus.UNPAID).build();

        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L))
                .thenReturn(List.of(classA1, classA2, classB1));
        when(schoolFeeRepository.findByTenantId(9L)).thenReturn(List.of(
                SchoolFee.builder().id(100L).classId(10L).build(),
                SchoolFee.builder().id(200L).classId(20L).build()));

        RecoveryBreakdown breakdown = adapter.recoveryBreakdown(9L, 1L, 10);

        assertThat(breakdown.overall().invoiced()).isEqualByComparingTo("250000");
        assertThat(breakdown.overall().outstanding()).isEqualByComparingTo("130000");

        assertThat(breakdown.byClass()).extracting(ClassRecovery::classId).containsExactly(10L, 20L);
        ClassRecovery classARecovery = breakdown.byClass().get(0);
        assertThat(classARecovery.invoiced()).isEqualByComparingTo("200000");
        assertThat(classARecovery.outstanding()).isEqualByComparingTo("80000");

        assertThat(breakdown.byPeriod()).extracting(PeriodRecovery::periodLabel)
                .containsExactly("Novembre 2025", "Octobre 2025");

        assertThat(breakdown.topDebtors()).hasSize(2);
        TopDebtor topDebtor = breakdown.topDebtors().get(0);
        assertThat(topDebtor.studentId()).isEqualTo(2L);
        assertThat(topDebtor.classId()).isEqualTo(10L);
        assertThat(topDebtor.outstanding()).isEqualByComparingTo("80000");
    }

    @Test
    void subsidyStats_countsExemptionsAndFamilyDiscountsAndFullySettledStudents() {
        when(exemptionRepository.findByTenantId(9L)).thenReturn(List.of(
                Exemption.builder().type(ExemptionType.TOTAL).active(true).build(),
                Exemption.builder().type(ExemptionType.PARTIAL).active(true).build(),
                Exemption.builder().type(ExemptionType.PARTIAL).active(false).build()));

        Invoice familyDiscounted = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("100000")).discountAmount(new BigDecimal("10000"))
                .discountReason("Réduction familiale : 2e enfant").status(InvoiceStatus.PAID).build();
        Invoice manualAdjustment = Invoice.builder().studentId(2L).feeId(300L)
                .totalAmount(new BigDecimal("100000")).discountAmount(new BigDecimal("5000"))
                .discountReason("Geste commercial").status(InvoiceStatus.PAID).build();
        // Student 1 : all MONTHLY invoices PAID (2 months) -> fully settled.
        Invoice monthly1 = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("50000")).status(InvoiceStatus.PAID).build();
        // Student 2 : one MONTHLY invoice still unpaid -> not fully settled.
        Invoice monthly2Unpaid = Invoice.builder().studentId(2L).feeId(300L)
                .totalAmount(new BigDecimal("50000")).status(InvoiceStatus.UNPAID).build();

        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L))
                .thenReturn(List.of(familyDiscounted, manualAdjustment, monthly1, monthly2Unpaid));
        when(schoolFeeRepository.findByTenantId(9L)).thenReturn(List.of(
                SchoolFee.builder().id(300L).type(FeeType.MONTHLY).build()));

        SubsidyStats stats = adapter.subsidyStats(9L, 1L);

        assertThat(stats.activeExemptionsCount()).isEqualTo(2);
        assertThat(stats.totalExemptions()).isEqualTo(1);
        assertThat(stats.partialExemptions()).isEqualTo(1);
        assertThat(stats.familyDiscountAmount()).isEqualByComparingTo("10000");
        assertThat(stats.studentsFullySettledMonthlyFees()).isEqualTo(1);
        // 5% of (familyDiscounted 100000 + monthly1 50000) = 7500
        assertThat(stats.estimatedEarlyPaymentSavings()).isEqualByComparingTo("7500.00");
    }
}
