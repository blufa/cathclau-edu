package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.students.EnrollmentValidationApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentValidationOnPaymentListenerTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock EnrollmentValidationApi enrollmentValidationApi;
    @InjectMocks EnrollmentValidationOnPaymentListener listener;

    private final InvoicePaidEvent event = new InvoicePaidEvent(1L, 7L, 50L, "REF-1");

    @Test
    void enrollmentFee_validatesEnrollment() {
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(Invoice.builder().id(50L).feeId(99L).build()));
        when(schoolFeeRepository.findById(99L)).thenReturn(Optional.of(
                SchoolFee.builder().id(99L).classId(3L).type(FeeType.ENROLLMENT).build()));

        listener.onInvoicePaid(event);

        verify(enrollmentValidationApi).validatePaidEnrollment(7L, 3L);
    }

    @Test
    void reEnrollmentFee_alsoValidatesEnrollment() {
        // Bug corrigé : une réinscription payée intégralement ne validait jamais l'inscription, restant
        // EN_ATTENTE indéfiniment — seul le type ENROLLMENT était pris en compte ici auparavant.
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(Invoice.builder().id(50L).feeId(99L).build()));
        when(schoolFeeRepository.findById(99L)).thenReturn(Optional.of(
                SchoolFee.builder().id(99L).classId(3L).type(FeeType.RE_ENROLLMENT).build()));

        listener.onInvoicePaid(event);

        verify(enrollmentValidationApi).validatePaidEnrollment(7L, 3L);
    }

    @Test
    void unrelatedFeeType_doesNothing() {
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(Invoice.builder().id(50L).feeId(99L).build()));
        when(schoolFeeRepository.findById(99L)).thenReturn(Optional.of(
                SchoolFee.builder().id(99L).classId(3L).type(FeeType.MONTHLY).build()));

        listener.onInvoicePaid(event);

        verify(enrollmentValidationApi, never()).validatePaidEnrollment(any(), any());
    }

    @Test
    void invoiceWithoutFeeId_doesNothing() {
        when(invoiceRepository.findById(50L)).thenReturn(Optional.of(Invoice.builder().id(50L).feeId(null).build()));

        listener.onInvoicePaid(event);

        verify(enrollmentValidationApi, never()).validatePaidEnrollment(any(), any());
    }
}
