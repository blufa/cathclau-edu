package gn.lacaeducation.payments.application.listener;

import gn.lacaeducation.payments.application.event.CashCollectedWithoutOpenSessionEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import gn.lacaeducation.payments.domain.CashierSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Détecteur B2 (§retour fondateurs, contrôle anti-fraude comptable). */
@ExtendWith(MockitoExtension.class)
class CashReconciliationGuardTest {

    @Mock CashierSessionRepository cashierSessionRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks CashReconciliationGuard guard;

    @Test
    void onPaymentSucceeded_onlineChannel_collectedByUserIdNull_neverChecked() {
        guard.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, 2L, new BigDecimal("50000"), null));

        verify(cashierSessionRepository, never()).findOpenByCashierId(org.mockito.ArgumentMatchers.any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentSucceeded_collectorHasOpenSession_noAlert() {
        when(cashierSessionRepository.findOpenByCashierId(99L))
                .thenReturn(Optional.of(CashierSession.builder().id(5L).cashierId(99L).build()));

        guard.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, 2L, new BigDecimal("50000"), 99L));

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onPaymentSucceeded_collectorHasNoOpenSession_publishesAlert() {
        when(cashierSessionRepository.findOpenByCashierId(99L)).thenReturn(Optional.empty());

        guard.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 1L, 2L, new BigDecimal("50000"), 99L));

        ArgumentCaptor<CashCollectedWithoutOpenSessionEvent> captor =
                ArgumentCaptor.forClass(CashCollectedWithoutOpenSessionEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(10L);
        assertThat(captor.getValue().cashierId()).isEqualTo(99L);
        assertThat(captor.getValue().paymentId()).isEqualTo(1L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50000");
    }
}
