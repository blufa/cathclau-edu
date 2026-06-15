package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.InvoiceGeneratedEvent;
import gn.lacaeducation.billing.application.event.InvoiceIssuedEvent;
import gn.lacaeducation.billing.application.event.InvoicePaidEvent;
import gn.lacaeducation.billing.application.port.in.InvoiceUseCase.CreateInvoiceCommand;
import gn.lacaeducation.billing.application.port.out.EnrollmentQueryPort;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.application.port.out.SchoolFeeRepository;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.common.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository repo;
    @Mock SchoolFeeRepository schoolFeeRepository;
    @Mock EnrollmentQueryPort enrollmentQueryPort;
    @Mock ApplicationEventPublisher eventPublisher;

    InvoiceService service() {
        return new InvoiceService(repo, schoolFeeRepository, enrollmentQueryPort, eventPublisher);
    }

    @Test
    void create_initialisesUnpaidWithFullRemainingBalanceAndPublishesEvents() {
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Invoice inv = service().create(new CreateInvoiceCommand(10L, 7L, 3L, "FAC-1", new BigDecimal("100.00")));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(inv.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("100.00");
        verify(eventPublisher).publishEvent(any(InvoiceGeneratedEvent.class));
        verify(eventPublisher).publishEvent(any(InvoiceIssuedEvent.class));
    }

    @Test
    void create_autoGeneratesTenantScopedReferenceWhenBlank() {
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().create(new CreateInvoiceCommand(42L, 7L, 3L, null, new BigDecimal("50.00")));

        assertThat(inv.getReference()).startsWith("FAC-42-");
    }

    @Test
    void applyPayment_partial_setsPartialStatusAndRecomputesBalance() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L)
                .totalAmount(new BigDecimal("100.00")).paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("100.00")).status(InvoiceStatus.UNPAID).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("30.00"));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PARTIAL);
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("70.00");
        verify(eventPublisher, never()).publishEvent(any(InvoicePaidEvent.class));
    }

    @Test
    void applyPayment_full_setsPaidAndPublishesInvoicePaidEvent() {
        Invoice existing = Invoice.builder().id(1L).tenantId(10L).studentId(7L).reference("FAC-1")
                .totalAmount(new BigDecimal("100.00")).paidAmount(new BigDecimal("40.00"))
                .remainingBalance(new BigDecimal("60.00")).status(InvoiceStatus.PARTIAL).build();
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice inv = service().applyPayment(1L, new BigDecimal("60.00"));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(inv.getRemainingBalance()).isEqualByComparingTo("0");
        ArgumentCaptor<InvoicePaidEvent> captor = ArgumentCaptor.forClass(InvoicePaidEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().invoiceId()).isEqualTo(1L);
        assertThat(captor.getValue().reference()).isEqualTo("FAC-1");
    }

    @Test
    void applyPayment_unknownInvoice_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().applyPayment(99L, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
