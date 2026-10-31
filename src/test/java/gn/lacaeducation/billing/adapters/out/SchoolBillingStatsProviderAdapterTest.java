package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.billing.SchoolBillingStatsProvider.BillingStats;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.ClassRecovery;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.PeriodRecovery;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.RecoveryBreakdown;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.SubsidyStats;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.TopDebtor;
import gn.lacaeducation.billing.SchoolBillingStatsProvider.UpcomingInstallment;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.billing.application.port.out.ExemptionRepository;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Exemption;
import gn.lacaeducation.billing.domain.ExemptionType;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolBillingStatsProviderAdapterTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock ExemptionRepository exemptionRepository;
    @Mock InstallmentRepository installmentRepository;
    @Mock EnrollmentQueryPort enrollmentQueryPort;
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
        assertThat(s.paidCount()).isEqualTo(1);
    }

    @Test
    void billingStats_creditedAmount_excludedFromCollected() {
        // §retour comptable, écart constaté en production : un crédit reporté (transfert de classe,
        // voir ClassTransferInvoiceListener#applyCredit) ajoute au paidAmount de la NOUVELLE facture
        // sans nouvel argent réellement reçu — l'ancien encaissement a déjà compté une fois sur
        // l'ancienne facture. Sans cette exclusion, "Encaissé" comptait ce même argent deux fois.
        Invoice creditedInvoice = Invoice.builder().totalAmount(new BigDecimal("100000"))
                .paidAmount(new BigDecimal("100000")).creditedAmount(new BigDecimal("100000"))
                .remainingBalance(new BigDecimal("0")).status(InvoiceStatus.PAID).build();
        Invoice realPayment = inv("50000", "50000", "0", InvoiceStatus.PAID);
        when(invoiceRepository.findByTenantId(9L)).thenReturn(List.of(creditedInvoice, realPayment));

        BillingStats s = adapter.billingStats(9L);

        assertThat(s.invoiced()).isEqualByComparingTo("150000");
        // 100 000 + 50 000 payés, moins 100 000 de crédit reporté (pas un nouvel encaissement) = 50 000.
        assertThat(s.collected()).isEqualByComparingTo("50000");
    }

    @Test
    void billingStats_noInvoices_isZero() {
        when(invoiceRepository.findByTenantId(9L)).thenReturn(List.of());
        BillingStats s = adapter.billingStats(9L);
        assertThat(s.invoiced()).isEqualByComparingTo("0");
        assertThat(s.recoveryRate()).isEqualByComparingTo("0");
        assertThat(s.unpaidCount()).isZero();
        assertThat(s.paidCount()).isZero();
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
    void subsidyStats_countsExemptionsAndExemptionDiscountsAndFullySettledStudents() {
        when(exemptionRepository.findByTenantId(9L)).thenReturn(List.of(
                Exemption.builder().type(ExemptionType.TOTAL).active(true).build(),
                Exemption.builder().type(ExemptionType.PARTIAL).active(true).build(),
                Exemption.builder().type(ExemptionType.PARTIAL).active(false).build()));

        Invoice exemptionDiscounted = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("100000")).discountAmount(new BigDecimal("10000"))
                .discountReason("Exonération : cas social").discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .status(InvoiceStatus.PAID).build();
        // Remise manuelle SANS discountSource (ex. ajustement ponctuel via le PATCH générique) — ne
        // doit jamais compter comme une remise d'exonération, seul discountSource fait foi.
        Invoice manualAdjustment = Invoice.builder().studentId(2L).feeId(300L)
                .totalAmount(new BigDecimal("100000")).discountAmount(new BigDecimal("5000"))
                .discountReason("Geste commercial").status(InvoiceStatus.PAID).build();
        // Remise "paiement anticipé" réellement appliquée (EarlyPaymentDiscountRule) — comptée à part.
        Invoice earlyPaymentDiscounted = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("50000")).discountAmount(new BigDecimal("2500"))
                .discountReason("Remise paiement anticipé : Paiement annuel (5%)").status(InvoiceStatus.PAID).build();
        // Student 1 : all MONTHLY invoices PAID (2 months) -> fully settled.
        Invoice monthly1 = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("50000")).status(InvoiceStatus.PAID).build();
        // Student 2 : one MONTHLY invoice still unpaid -> not fully settled.
        Invoice monthly2Unpaid = Invoice.builder().studentId(2L).feeId(300L)
                .totalAmount(new BigDecimal("50000")).status(InvoiceStatus.UNPAID).build();

        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L))
                .thenReturn(List.of(exemptionDiscounted, manualAdjustment, earlyPaymentDiscounted, monthly1, monthly2Unpaid));
        when(schoolFeeRepository.findByTenantId(9L)).thenReturn(List.of(
                SchoolFee.builder().id(300L).type(FeeType.MONTHLY).build()));

        SubsidyStats stats = adapter.subsidyStats(9L, 1L);

        assertThat(stats.activeExemptionsCount()).isEqualTo(2);
        assertThat(stats.totalExemptions()).isEqualTo(1);
        assertThat(stats.partialExemptions()).isEqualTo(1);
        assertThat(stats.exemptionDiscountAmount()).isEqualByComparingTo("10000");
        assertThat(stats.exemptionDiscountInvoiceCount()).isEqualTo(1);
        assertThat(stats.exemptionDiscountInvoicePaidCount()).isEqualTo(1);
        assertThat(stats.studentsFullySettledMonthlyFees()).isEqualTo(1);
        assertThat(stats.earlyPaymentDiscountApplied()).isEqualByComparingTo("2500");
    }

    @Test
    void subsidyStats_exemptionDiscountInvoiceCount_distinguishesPaidFromUnpaid() {
        when(exemptionRepository.findByTenantId(9L)).thenReturn(List.of());
        Invoice paidDiscounted = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("100000")).discountAmount(new BigDecimal("10000"))
                .discountReason("Exonération : 3e enfant").discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .status(InvoiceStatus.PAID).build();
        Invoice unpaidDiscounted = Invoice.builder().studentId(1L).feeId(300L)
                .totalAmount(new BigDecimal("100000")).discountAmount(new BigDecimal("10000"))
                .discountReason("Exonération : 3e enfant").discountSource(gn.lacaeducation.billing.domain.DiscountSource.EXEMPTION)
                .status(InvoiceStatus.UNPAID).build();
        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L))
                .thenReturn(List.of(paidDiscounted, unpaidDiscounted));
        when(schoolFeeRepository.findByTenantId(9L)).thenReturn(List.of(
                SchoolFee.builder().id(300L).type(FeeType.MONTHLY).build()));

        SubsidyStats stats = adapter.subsidyStats(9L, 1L);

        assertThat(stats.exemptionDiscountInvoiceCount()).isEqualTo(2);
        assertThat(stats.exemptionDiscountInvoicePaidCount()).isEqualTo(1);
    }

    /**
     * Retour fondateurs : une exonération TOTALE (100%) ne produit jamais de facture (voir
     * InvoiceService#buildAndSaveInvoice), donc rien dans exemptionDiscountAmount pour elle — ce test
     * vérifie que sa valeur est quand même estimée séparément (totalExemptionEstimatedValue), à partir
     * des échéances réellement configurées pour le frais MENSUALITE de la classe actuelle de l'élève.
     */
    @Test
    void subsidyStats_totalExemption_estimatesValueFromStudentsClassMonthlyFee() {
        when(exemptionRepository.findByTenantId(9L)).thenReturn(List.of(
                Exemption.builder().studentId(5L).type(ExemptionType.TOTAL).active(true).build()));
        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L)).thenReturn(List.of());
        when(schoolFeeRepository.findByTenantId(9L)).thenReturn(List.of());
        when(enrollmentQueryPort.findActiveClassId(5L)).thenReturn(20L);
        when(schoolFeeRepository.findByClassIdAndType(20L, FeeType.MONTHLY))
                .thenReturn(List.of(SchoolFee.builder().id(300L).classId(20L).type(FeeType.MONTHLY).build()));
        when(installmentRepository.findByFeeId(300L)).thenReturn(List.of(
                Installment.builder().id(1L).feeId(300L).amount(new BigDecimal("100000")).build(),
                Installment.builder().id(2L).feeId(300L).amount(new BigDecimal("100000")).build()));

        SubsidyStats stats = adapter.subsidyStats(9L, 1L);

        assertThat(stats.totalExemptionEstimatedValue()).isEqualByComparingTo("200000");
    }

    @Test
    void subsidyStats_totalExemption_feeTypesExcludesMonthly_estimatesZero() {
        when(exemptionRepository.findByTenantId(9L)).thenReturn(List.of(
                Exemption.builder().studentId(5L).type(ExemptionType.TOTAL).active(true)
                        .feeTypes(List.of(FeeType.EXAM)).build()));
        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L)).thenReturn(List.of());
        when(schoolFeeRepository.findByTenantId(9L)).thenReturn(List.of());

        SubsidyStats stats = adapter.subsidyStats(9L, 1L);

        assertThat(stats.totalExemptionEstimatedValue()).isEqualByComparingTo("0");
        verify(enrollmentQueryPort, never()).findActiveClassId(any());
    }

    @Test
    void upcomingInstallments_delegatesToRepositoryWithWindowAndLimit() {
        // Le repository fait déjà le filtrage tenant/date/statut (jointure school_fees) — l'adapter
        // ne fait que borner le nombre de résultats et mapper vers le DTO public.
        when(installmentRepository.findUpcomingByTenant(any(), any(), any())).thenReturn(List.of(
                Installment.builder().id(1L).feeId(100L).label("Mensualité Novembre")
                        .dueDate(LocalDate.of(2026, 11, 5)).amount(new BigDecimal("50000")).build(),
                Installment.builder().id(2L).feeId(100L).label("Mensualité Décembre")
                        .dueDate(LocalDate.of(2026, 12, 5)).amount(new BigDecimal("50000")).build(),
                Installment.builder().id(3L).feeId(200L).label("Mensualité Janvier")
                        .dueDate(LocalDate.of(2027, 1, 5)).amount(new BigDecimal("50000")).build()));

        List<UpcomingInstallment> result = adapter.upcomingInstallments(9L, 60, 2);

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        org.mockito.Mockito.verify(installmentRepository).findUpcomingByTenant(org.mockito.ArgumentMatchers.eq(9L), from.capture(), to.capture());
        assertThat(to.getValue()).isEqualTo(from.getValue().plusDays(60));

        assertThat(result).hasSize(2); // borné par limit=2 malgré 3 échéances retournées par le repo
        assertThat(result.get(0).label()).isEqualTo("Mensualité Novembre");
        assertThat(result.get(0).amount()).isEqualByComparingTo("50000");
    }

    @Test
    void upcomingInstallments_none_returnsEmptyList() {
        when(installmentRepository.findUpcomingByTenant(any(), any(), any())).thenReturn(List.of());
        assertThat(adapter.upcomingInstallments(9L, 30, 5)).isEmpty();
    }

    @Test
    void writtenOffAmountByStudent_sumsCancelledInvoicesRemainderOnly_ignoresPaidAndOtherStudents() {
        Invoice cancelledPartial = Invoice.builder().studentId(1L)
                .totalAmount(new BigDecimal("100000")).paidAmount(new BigDecimal("20000"))
                .status(InvoiceStatus.CANCELLED).build();
        Invoice cancelledUnpaid = Invoice.builder().studentId(1L)
                .totalAmount(new BigDecimal("50000")).paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.CANCELLED).build();
        // Not touched: still PAID, and a different (non-requested) student's cancelled invoice.
        Invoice stillPaid = Invoice.builder().studentId(1L)
                .totalAmount(new BigDecimal("30000")).paidAmount(new BigDecimal("30000"))
                .status(InvoiceStatus.PAID).build();
        Invoice otherStudentCancelled = Invoice.builder().studentId(2L)
                .totalAmount(new BigDecimal("40000")).paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.CANCELLED).build();

        when(invoiceRepository.findByTenantIdAndAcademicYearId(9L, 1L))
                .thenReturn(List.of(cancelledPartial, cancelledUnpaid, stillPaid, otherStudentCancelled));

        var result = adapter.writtenOffAmountByStudent(9L, 1L, List.of(1L));

        // (100000-20000) + (50000-0) = 130000 ; student 2 excluded (not requested).
        assertThat(result).containsOnlyKeys(1L);
        assertThat(result.get(1L)).isEqualByComparingTo("130000");
    }

    @Test
    void writtenOffAmountByStudent_emptyStudentIds_returnsEmptyMap() {
        assertThat(adapter.writtenOffAmountByStudent(9L, 1L, List.of())).isEmpty();
    }
}
