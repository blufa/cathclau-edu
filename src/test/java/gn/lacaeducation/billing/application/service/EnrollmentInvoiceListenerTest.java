package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.CreateInvoiceCommand;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.FeeType;
import gn.lacaeducation.billing.domain.SchoolFee;
import gn.lacaeducation.students.application.event.EnrollmentCreatedEvent;
import gn.lacaeducation.students.EnrollmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentInvoiceListenerTest {

    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock InvoiceUseCase invoiceUseCase;
    @Mock gn.lacaeducation.common.TenantInfoProvider tenantInfoProvider;
    @InjectMocks EnrollmentInvoiceListener listener;

    private final EnrollmentCreatedEvent event = new EnrollmentCreatedEvent(7L, 3L, 1L, 5L, EnrollmentType.NEW);

    @Test
    void createsEnrollmentInvoiceWhenFeeConfigured() {
        when(schoolFeeRepository.findByClassIdAndType(3L, FeeType.ENROLLMENT)).thenReturn(List.of(
                SchoolFee.builder().id(99L).tenantId(10L).amount(new BigDecimal("250.00")).mandatory(true).build()));
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 99L)).thenReturn(false);

        listener.onEnrollmentCreated(event);

        verify(invoiceUseCase).create(any(CreateInvoiceCommand.class));
    }

    @Test
    void skipsWhenNoEnrollmentFee() {
        when(schoolFeeRepository.findByClassIdAndType(3L, FeeType.ENROLLMENT)).thenReturn(List.of());

        listener.onEnrollmentCreated(event);

        verify(invoiceUseCase, never()).create(any());
    }

    @Test
    void skipsOptionalFee_noAutomaticInvoice() {
        // §13 cahier des charges.
        when(schoolFeeRepository.findByClassIdAndType(3L, FeeType.ENROLLMENT)).thenReturn(List.of(
                SchoolFee.builder().id(99L).tenantId(10L).amount(new BigDecimal("250.00")).mandatory(false).build()));

        listener.onEnrollmentCreated(event);

        verify(invoiceUseCase, never()).create(any());
    }

    @Test
    void idempotent_skipsWhenInvoiceAlreadyExists() {
        when(schoolFeeRepository.findByClassIdAndType(3L, FeeType.ENROLLMENT)).thenReturn(List.of(
                SchoolFee.builder().id(99L).tenantId(10L).amount(new BigDecimal("250.00")).mandatory(true).build()));
        when(invoiceUseCase.existsByStudentIdAndFeeId(7L, 99L)).thenReturn(true);

        listener.onEnrollmentCreated(event);

        verify(invoiceUseCase, never()).create(any());
    }
}
