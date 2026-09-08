package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.EcheanceDepasseeEvent;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.application.port.out.InvoiceRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import gn.lacaeducation.platform.TenantListPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineCheckSchedulerTest {

    @Mock InstallmentRepository installmentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock TenantListPort tenantListPort;

    OverdueInstallmentProcessor processor;
    DeadlineCheckScheduler scheduler;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        processor = new OverdueInstallmentProcessor(installmentRepository, invoiceRepository, eventPublisher);
        scheduler = new DeadlineCheckScheduler(tenantListPort, processor);
        when(tenantListPort.listActiveTenantIds()).thenReturn(List.of(new TenantListPort.TenantRef(1L)));
    }

    @Test
    void marksOverdueAndPublishesEventPerUnpaidStudent() {
        Installment pending = Installment.builder().id(5L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(2))
                .status(InstallmentStatus.PENDING).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(pending));
        Invoice unpaid = Invoice.builder().id(9L).studentId(42L).installmentId(5L)
                .reference("EC-INS-1").status(InvoiceStatus.UNPAID).build();
        when(invoiceRepository.findByInstallmentId(5L)).thenReturn(List.of(unpaid));

        scheduler.checkOverdueDeadlines();

        ArgumentCaptor<Installment> saved = ArgumentCaptor.forClass(Installment.class);
        verify(installmentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(InstallmentStatus.OVERDUE);

        ArgumentCaptor<EcheanceDepasseeEvent> evt = ArgumentCaptor.forClass(EcheanceDepasseeEvent.class);
        verify(eventPublisher).publishEvent(evt.capture());
        assertThat(evt.getValue().tenantId()).isEqualTo(1L);
        assertThat(evt.getValue().studentId()).isEqualTo(42L);
        assertThat(evt.getValue().installmentId()).isEqualTo(5L);
    }

    @Test
    void alreadyOverdue_publishesReminderWithoutResaving() {
        Installment overdue = Installment.builder().id(6L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(10))
                .status(InstallmentStatus.OVERDUE).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(overdue));
        Invoice unpaid = Invoice.builder().id(10L).studentId(43L).installmentId(6L)
                .reference("EC-INS-2").status(InvoiceStatus.PARTIAL).build();
        when(invoiceRepository.findByInstallmentId(6L)).thenReturn(List.of(unpaid));

        scheduler.checkOverdueDeadlines();

        verify(installmentRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(EcheanceDepasseeEvent.class));
    }

    @Test
    void noUnpaidInvoice_publishesNothing() {
        Installment pending = Installment.builder().id(7L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(2))
                .status(InstallmentStatus.PENDING).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(pending));
        when(invoiceRepository.findByInstallmentId(7L)).thenReturn(List.of());

        scheduler.checkOverdueDeadlines();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
