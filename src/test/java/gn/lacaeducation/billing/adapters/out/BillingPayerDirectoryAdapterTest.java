package gn.lacaeducation.billing.adapters.out;

import gn.lacaeducation.academics.AcademicsDataProvider;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.academics.AcademicYearPublicInfo;
import gn.lacaeducation.iam.UserPublicInfoProvider;
import gn.lacaeducation.payments.PayerDirectory.UnpaidInvoiceInfo;
import gn.lacaeducation.students.StudentDataProvider;
import gn.lacaeducation.students.StudentPublicInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingPayerDirectoryAdapterTest {

    @Mock StudentDataProvider studentDataProvider;
    @Mock AcademicsDataProvider academicsDataProvider;
    @Mock UserPublicInfoProvider userPublicInfoProvider;
    @Mock InvoiceRepository invoiceRepository;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @InjectMocks BillingPayerDirectoryAdapter adapter;

    // ── unpaidInvoices (§retour comptable, point 1 : reste à payer énuméré dans le SMS d'encaissement) ──

    @Test
    void unpaidInvoices_onlyUnpaidAndPartial_labelledWithFeeTypeAndPeriod() {
        Invoice monthly = Invoice.builder().id(1L).studentId(7L).feeId(3L)
                .status(InvoiceStatus.PARTIAL).remainingBalance(new BigDecimal("20000"))
                .periodLabel("Novembre 2026").issueDate(LocalDate.of(2026, 11, 1)).build();
        Invoice canteen = Invoice.builder().id(2L).studentId(7L).feeId(4L)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("15000"))
                .issueDate(LocalDate.of(2026, 11, 5)).build();
        Invoice paid = Invoice.builder().id(3L).studentId(7L).feeId(3L)
                .status(InvoiceStatus.PAID).remainingBalance(BigDecimal.ZERO)
                .issueDate(LocalDate.of(2026, 10, 1)).build();
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of(monthly, canteen, paid));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                SchoolFee.builder().id(3L).type(FeeType.MONTHLY).build()));
        when(schoolFeeRepository.findById(4L)).thenReturn(Optional.of(
                SchoolFee.builder().id(4L).type(FeeType.CANTEEN).build()));

        List<UnpaidInvoiceInfo> result = adapter.unpaidInvoices(7L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UnpaidInvoiceInfo::label)
                .containsExactly("Mensualité — Novembre 2026", "Cantine");
        assertThat(result).extracting(UnpaidInvoiceInfo::remaining)
                .containsExactly(new BigDecimal("20000"), new BigDecimal("15000"));
    }

    @Test
    void unpaidInvoices_otherFeeWithCustomLabel_usesConfiguredLabelNotGenericAutre() {
        Invoice uniform = Invoice.builder().id(1L).studentId(7L).feeId(5L)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("30000"))
                .issueDate(LocalDate.of(2026, 9, 1)).build();
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of(uniform));
        when(schoolFeeRepository.findById(5L)).thenReturn(Optional.of(
                SchoolFee.builder().id(5L).type(FeeType.OTHER).label("Tenue scolaire").build()));

        List<UnpaidInvoiceInfo> result = adapter.unpaidInvoices(7L);

        assertThat(result).extracting(UnpaidInvoiceInfo::label).containsExactly("Tenue scolaire");
    }

    @Test
    void unpaidInvoices_noneOutstanding_returnsEmpty() {
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of());

        assertThat(adapter.unpaidInvoices(7L)).isEmpty();
    }

    /** Régression : le "reste à payer" du reçu de paiement ne doit compter que les factures de
     * l'année scolaire COURANTE — une dette d'une année révolue (jamais annulée en base) ne doit pas
     * gonfler le total affiché au comptable (§retour comptable). */
    @Test
    void unpaidInvoices_excludesInvoicesFromAPastAcademicYear() {
        Invoice currentYear = Invoice.builder().id(1L).studentId(7L).feeId(3L).academicYearId(20L)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("50000"))
                .issueDate(LocalDate.of(2026, 10, 1)).build();
        Invoice pastYear = Invoice.builder().id(2L).studentId(7L).feeId(3L).academicYearId(19L)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("1000000"))
                .issueDate(LocalDate.of(2025, 10, 1)).build();
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of(currentYear, pastYear));
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(
                new StudentPublicInfo(7L, 1L, "Awa", "Diallo", null, null, "MAT-1", null, null, null)));
        when(academicsDataProvider.currentAcademicYear(1L)).thenReturn(Optional.of(
                new AcademicYearPublicInfo(20L, "2026-2027", null, null)));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                SchoolFee.builder().id(3L).type(FeeType.MONTHLY).build()));

        List<UnpaidInvoiceInfo> result = adapter.unpaidInvoices(7L);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(UnpaidInvoiceInfo::remaining).containsExactly(new BigDecimal("50000"));
    }

    @Test
    void unpaidInvoices_invoiceWithNoAcademicYear_stillIncluded() {
        Invoice legacy = Invoice.builder().id(1L).studentId(7L).feeId(3L).academicYearId(null)
                .status(InvoiceStatus.UNPAID).remainingBalance(new BigDecimal("50000"))
                .issueDate(LocalDate.of(2026, 10, 1)).build();
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of(legacy));
        when(studentDataProvider.findStudent(7L)).thenReturn(Optional.of(
                new StudentPublicInfo(7L, 1L, "Awa", "Diallo", null, null, "MAT-1", null, null, null)));
        when(academicsDataProvider.currentAcademicYear(1L)).thenReturn(Optional.of(
                new AcademicYearPublicInfo(20L, "2026-2027", null, null)));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                SchoolFee.builder().id(3L).type(FeeType.MONTHLY).build()));

        assertThat(adapter.unpaidInvoices(7L)).hasSize(1);
    }

    // ── studentInvoices (§retour comptable : tableau complet du reçu, toutes les factures de l'élève) ──

    @Test
    void studentInvoices_excludesCancelled_flagsPaidStatusPerRow() {
        Invoice paid = Invoice.builder().id(1L).studentId(7L).feeId(3L).reference("FAC-1")
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("50000"))
                .issueDate(LocalDate.of(2026, 9, 1)).build();
        Invoice unpaid = Invoice.builder().id(2L).studentId(7L).feeId(3L).reference("FAC-2")
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("50000"))
                .periodLabel("Octobre 2026").issueDate(LocalDate.of(2026, 10, 1)).build();
        Invoice cancelled = Invoice.builder().id(3L).studentId(7L).feeId(3L).reference("FAC-3")
                .status(InvoiceStatus.CANCELLED).totalAmount(new BigDecimal("50000"))
                .issueDate(LocalDate.of(2026, 8, 1)).build();
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of(paid, unpaid, cancelled));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(
                SchoolFee.builder().id(3L).type(FeeType.MONTHLY).build()));

        var result = adapter.studentInvoices(7L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(gn.lacaeducation.payments.PayerDirectory.InvoiceRowInfo::reference)
                .containsExactly("FAC-1", "FAC-2");
        assertThat(result).extracting(gn.lacaeducation.payments.PayerDirectory.InvoiceRowInfo::paid)
                .containsExactly(true, false);
        assertThat(result.get(1).label()).isEqualTo("Mensualité — Octobre 2026");
    }

    /**
     * Ordre du tableau de factures du reçu (§retour comptable) : inscription d'abord, puis la
     * scolarité (tranches, chronologique), puis les frais annexes — PAS un simple tri par date
     * d'émission. Les factures ci-dessous sont volontairement créées dans un ordre de date qui ne
     * correspond PAS à l'ordre attendu, pour vérifier que c'est bien le type de frais qui prime : le
     * transport (souscrit avant tout le reste) doit malgré ça finir en bas, après la mensualité.
     */
    @Test
    void studentInvoices_ordersByFeeTypeGroupThenDate_notByIssueDateAlone() {
        Invoice transport = Invoice.builder().id(1L).studentId(7L).feeId(5L).reference("FAC-TRANS")
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("30000"))
                .issueDate(LocalDate.of(2026, 1, 1)).build();
        Invoice monthly = Invoice.builder().id(2L).studentId(7L).feeId(3L).reference("FAC-MENS")
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("50000"))
                .issueDate(LocalDate.of(2026, 10, 1)).build();
        Invoice enrollment = Invoice.builder().id(3L).studentId(7L).feeId(4L).reference("FAC-INS")
                .status(InvoiceStatus.PAID).totalAmount(new BigDecimal("100000"))
                .issueDate(LocalDate.of(2026, 11, 1)).build();
        when(invoiceRepository.findByStudentId(7L)).thenReturn(List.of(transport, monthly, enrollment));
        when(schoolFeeRepository.findById(5L)).thenReturn(Optional.of(SchoolFee.builder().id(5L).type(FeeType.TRANSPORT).build()));
        when(schoolFeeRepository.findById(3L)).thenReturn(Optional.of(SchoolFee.builder().id(3L).type(FeeType.MONTHLY).build()));
        when(schoolFeeRepository.findById(4L)).thenReturn(Optional.of(SchoolFee.builder().id(4L).type(FeeType.ENROLLMENT).build()));

        var result = adapter.studentInvoices(7L);

        assertThat(result).extracting(gn.lacaeducation.payments.PayerDirectory.InvoiceRowInfo::reference)
                .containsExactly("FAC-INS", "FAC-MENS", "FAC-TRANS");
    }
}
