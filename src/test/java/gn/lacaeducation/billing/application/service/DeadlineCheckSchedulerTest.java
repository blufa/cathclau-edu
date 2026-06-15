package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.event.EcheanceDepasseeEvent;
import gn.lacaeducation.billing.application.port.out.InstallmentRepository;
import gn.lacaeducation.billing.domain.Installment;
import gn.lacaeducation.billing.domain.InstallmentStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineCheckSchedulerTest {

    @Mock InstallmentRepository installmentRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @InjectMocks DeadlineCheckScheduler scheduler;

    @Test
    void marksOverdueAndPublishesEvent() {
        Installment pending = Installment.builder().id(5L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(2))
                .status(InstallmentStatus.PENDING).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(pending));

        scheduler.checkOverdueDeadlines();

        ArgumentCaptor<Installment> saved = ArgumentCaptor.forClass(Installment.class);
        verify(installmentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(InstallmentStatus.OVERDUE);

        ArgumentCaptor<EcheanceDepasseeEvent> evt = ArgumentCaptor.forClass(EcheanceDepasseeEvent.class);
        verify(eventPublisher).publishEvent(evt.capture());
        assertThat(evt.getValue().installmentId()).isEqualTo(5L);
    }

    @Test
    void alreadyOverdue_publishesReminderWithoutResaving() {
        Installment overdue = Installment.builder().id(6L).feeId(1L)
                .amount(new BigDecimal("60.00")).dueDate(LocalDate.now().minusDays(10))
                .status(InstallmentStatus.OVERDUE).build();
        when(installmentRepository.findOverdue(any(LocalDate.class))).thenReturn(List.of(overdue));

        scheduler.checkOverdueDeadlines();

        verify(installmentRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(EcheanceDepasseeEvent.class));
    }
}
