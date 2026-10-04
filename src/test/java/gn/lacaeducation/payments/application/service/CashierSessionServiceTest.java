package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.payments.application.event.CashierSessionClosedEvent;
import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.domain.CashierSession;
import gn.lacaeducation.payments.domain.CashierSessionStatus;
import gn.lacaeducation.payments.domain.Payment;
import gn.lacaeducation.payments.domain.PaymentChannel;
import gn.lacaeducation.payments.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashierSessionServiceTest {

    @Mock CashierSessionRepository repo;
    @Mock PaymentRepository paymentRepo;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CashierSessionService service;

    private CashierSession openSession() {
        return CashierSession.builder().id(1L).tenantId(10L).cashierId(42L)
                .openedAt(LocalDateTime.of(2026, 10, 3, 8, 0)).status(CashierSessionStatus.OPEN).build();
    }

    @Test
    void open_alreadyOpenForCashier_throws() {
        when(repo.findOpenByCashierId(42L)).thenReturn(Optional.of(openSession()));

        assertThatThrownBy(() -> service.open(10L, 42L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void close_computesDiscrepancy_andPublishesEventEvenWhenZero() {
        when(repo.findById(1L)).thenReturn(Optional.of(openSession()));
        when(paymentRepo.findByCollectedByUserIdAndPaidAtBetween(any(), any(), any())).thenReturn(List.of(
                Payment.builder().amount(new BigDecimal("50000")).channel(PaymentChannel.CASH).status(PaymentStatus.SUCCEEDED).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        CashierSession closed = service.close(1L, new BigDecimal("50000"), null);

        assertThat(closed.getSystemTotal()).isEqualByComparingTo("50000");
        assertThat(closed.getDiscrepancy()).isEqualByComparingTo("0");
        assertThat(closed.getStatus()).isEqualTo(CashierSessionStatus.CLOSED);

        ArgumentCaptor<CashierSessionClosedEvent> captor = ArgumentCaptor.forClass(CashierSessionClosedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().discrepancy()).isEqualByComparingTo("0");
        assertThat(captor.getValue().cashierId()).isEqualTo(42L);
        assertThat(captor.getValue().sessionId()).isEqualTo(1L);
    }

    @Test
    void close_physicalCountBelowSystemTotal_negativeDiscrepancyStillPublished() {
        // §retour fondateurs : de l'argent manque (vol présumé) — le signal central de ce plan.
        when(repo.findById(1L)).thenReturn(Optional.of(openSession()));
        when(paymentRepo.findByCollectedByUserIdAndPaidAtBetween(any(), any(), any())).thenReturn(List.of(
                Payment.builder().amount(new BigDecimal("50000")).channel(PaymentChannel.CASH).status(PaymentStatus.SUCCEEDED).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        CashierSession closed = service.close(1L, new BigDecimal("30000"), "Comptant inférieur");

        assertThat(closed.getDiscrepancy()).isEqualByComparingTo("-20000");
        ArgumentCaptor<CashierSessionClosedEvent> captor = ArgumentCaptor.forClass(CashierSessionClosedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().discrepancy()).isEqualByComparingTo("-20000");
    }

    @Test
    void close_ignoresNonCashOrFailedPayments_inSystemTotal() {
        when(repo.findById(1L)).thenReturn(Optional.of(openSession()));
        when(paymentRepo.findByCollectedByUserIdAndPaidAtBetween(any(), any(), any())).thenReturn(List.of(
                Payment.builder().amount(new BigDecimal("50000")).channel(PaymentChannel.ORANGE_MONEY).status(PaymentStatus.SUCCEEDED).build(),
                Payment.builder().amount(new BigDecimal("20000")).channel(PaymentChannel.CASH).status(PaymentStatus.FAILED).build(),
                Payment.builder().amount(new BigDecimal("10000")).channel(PaymentChannel.CASH).status(PaymentStatus.SUCCEEDED).build()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        CashierSession closed = service.close(1L, new BigDecimal("10000"), null);

        assertThat(closed.getSystemTotal()).isEqualByComparingTo("10000");
    }

    @Test
    void close_alreadyClosed_throws() {
        CashierSession alreadyClosed = openSession();
        alreadyClosed.setStatus(CashierSessionStatus.CLOSED);
        when(repo.findById(1L)).thenReturn(Optional.of(alreadyClosed));

        assertThatThrownBy(() -> service.close(1L, BigDecimal.ZERO, null)).isInstanceOf(BusinessRuleException.class);
    }
}
