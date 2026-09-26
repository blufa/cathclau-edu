package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.EnrollmentBillingUseCase.GenerateAllFeesCommand;
import gn.lacaeducation.billing.application.port.in.EnrollmentBillingUseCase.GeneratedFeeInvoices;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.students.EnrollmentType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentBillingServiceTest {

    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock InstallmentRepository installmentRepository;
    @Mock InvoiceUseCase invoiceUseCase;
    @Mock TenantInfoProvider tenantInfoProvider;
    @InjectMocks EnrollmentBillingService service;

    private SchoolFee fee(Long id, FeeType type, Long classId, Long routeId) {
        return SchoolFee.builder().id(id).tenantId(1L).classId(classId).routeId(routeId)
                .type(type).amount(BigDecimal.valueOf(50_000)).build();
    }

    @Test
    void generatesSimpleInvoice_forFeeWithoutInstallments() {
        SchoolFee enrollmentFee = fee(1L, FeeType.ENROLLMENT, 10L, null);
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of(enrollmentFee));
        when(installmentRepository.findByFeeId(1L)).thenReturn(List.of());
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 1L)).thenReturn(false);
        Invoice created = Invoice.builder().id(100L).feeId(1L).build();
        when(invoiceUseCase.create(any())).thenReturn(created);

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, false, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).invoices()).containsExactly(created);
        verify(invoiceUseCase).create(any());
    }

    @Test
    void skipsInvoiceCreation_whenAlreadyExists() {
        SchoolFee examFee = fee(2L, FeeType.EXAM, 10L, null);
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of(examFee));
        when(installmentRepository.findByFeeId(2L)).thenReturn(List.of());
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 2L)).thenReturn(true);

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, false, null));

        assertThat(result.get(0).invoices()).isEmpty();
        verify(invoiceUseCase, never()).create(any());
    }

    @Test
    void usesGenerateMonthly_forFeeWithInstallments() {
        SchoolFee monthlyFee = fee(3L, FeeType.MONTHLY, 10L, null);
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of(monthlyFee));
        when(installmentRepository.findByFeeId(3L)).thenReturn(List.of(Installment.builder().id(30L).feeId(3L).build()));
        List<Invoice> generated = List.of(Invoice.builder().id(101L).feeId(3L).build(), Invoice.builder().id(102L).feeId(3L).build());
        when(invoiceUseCase.generateMonthly(any())).thenReturn(generated);

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, false, null));

        assertThat(result.get(0).invoices()).hasSize(2);
        verify(invoiceUseCase).generateMonthly(any());
        verify(invoiceUseCase, never()).create(any());
    }

    @Test
    void includesCanteenFee_unconditionally_whenConfiguredOnClass() {
        // Plus de case à cocher "Cantine" côté formulaire (§retour comptable) — un frais cantine
        // configuré sur la classe est toujours généré, comme n'importe quel autre frais de la classe.
        SchoolFee canteenFee = fee(4L, FeeType.CANTEEN, 10L, null);
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of(canteenFee));
        when(installmentRepository.findByFeeId(4L)).thenReturn(List.of());
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 4L)).thenReturn(false);
        when(invoiceUseCase.create(any())).thenReturn(Invoice.builder().id(104L).feeId(4L).build());

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, false, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fee().getType()).isEqualTo(FeeType.CANTEEN);
    }

    @Test
    void newEnrollment_excludesReEnrollmentFee_whenClassHasBoth() {
        SchoolFee enrollmentFee = fee(1L, FeeType.ENROLLMENT, 10L, null);
        SchoolFee reEnrollmentFee = fee(5L, FeeType.RE_ENROLLMENT, 10L, null);
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of(enrollmentFee, reEnrollmentFee));
        when(installmentRepository.findByFeeId(1L)).thenReturn(List.of());
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 1L)).thenReturn(false);
        when(invoiceUseCase.create(any())).thenReturn(Invoice.builder().id(100L).feeId(1L).build());

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, false, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fee().getType()).isEqualTo(FeeType.ENROLLMENT);
    }

    @Test
    void reEnrollment_excludesEnrollmentFee_whenClassHasBoth() {
        SchoolFee enrollmentFee = fee(1L, FeeType.ENROLLMENT, 10L, null);
        SchoolFee reEnrollmentFee = fee(5L, FeeType.RE_ENROLLMENT, 10L, null);
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of(enrollmentFee, reEnrollmentFee));
        when(installmentRepository.findByFeeId(5L)).thenReturn(List.of());
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 5L)).thenReturn(false);
        when(invoiceUseCase.create(any())).thenReturn(Invoice.builder().id(105L).feeId(5L).build());

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.RE_ENROLLMENT, false, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fee().getType()).isEqualTo(FeeType.RE_ENROLLMENT);
    }

    @Test
    void includesTransportFee_whenWantedAndFound() {
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of());
        SchoolFee transportFee = fee(9L, FeeType.TRANSPORT, null, 55L);
        when(schoolFeeRepository.findById(9L)).thenReturn(Optional.of(transportFee));
        when(installmentRepository.findByFeeId(9L)).thenReturn(List.of());
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 9L)).thenReturn(false);
        when(invoiceUseCase.create(any())).thenReturn(Invoice.builder().id(103L).feeId(9L).build());

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, true, 9L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fee().getType()).isEqualTo(FeeType.TRANSPORT);
    }

    @Test
    void ignoresTransportFeeId_whenItIsNotActuallyTransportType() {
        when(schoolFeeRepository.findByClassId(10L)).thenReturn(List.of());
        SchoolFee wrongTypeFee = fee(9L, FeeType.MONTHLY, 10L, null);
        when(schoolFeeRepository.findById(9L)).thenReturn(Optional.of(wrongTypeFee));

        List<GeneratedFeeInvoices> result = service.generateAllForEnrollment(
                new GenerateAllFeesCommand(1L, 7L, 10L, 5L, EnrollmentType.NEW, true, 9L));

        assertThat(result).isEmpty();
        verify(invoiceUseCase, never()).create(any());
    }
}
