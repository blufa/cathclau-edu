package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.payments.application.event.GroupedInvoiceSettlementEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
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
class InvoicePaymentListenerTest {

    @Mock InvoiceUseCase invoiceUseCase;
    @InjectMocks InvoicePaymentListener listener;

    @Test
    void appliesPaymentToInvoiceOnSuccess() {
        when(invoiceUseCase.existsById(3L)).thenReturn(true);

        listener.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, 3L, new BigDecimal("60.00"), null));

        verify(invoiceUseCase).applyPayment(3L, new BigDecimal("60.00"));
    }

    @Test
    void skipsWhenInvoiceIdMissing() {
        listener.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, null, new BigDecimal("60.00"), null));

        verify(invoiceUseCase, never()).applyPayment(any(), any());
    }

    @Test
    void skipsWhenInvoiceUnknown() {
        when(invoiceUseCase.existsById(3L)).thenReturn(false);

        listener.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, 3L, new BigDecimal("60.00"), null));

        verify(invoiceUseCase, never()).applyPayment(any(), any());
    }

    @Test
    void reversesPaymentOnInvoiceOnRefund() {
        when(invoiceUseCase.existsById(3L)).thenReturn(true);

        listener.onPaymentRefunded(new PaymentRefundedEvent(10L, 7L, 100L, 3L, new BigDecimal("60.00")));

        verify(invoiceUseCase).reversePayment(3L, new BigDecimal("60.00"));
        verify(invoiceUseCase, never()).applyPayment(any(), any());
    }

    @Test
    void refund_skipsWhenInvoiceIdMissing() {
        listener.onPaymentRefunded(new PaymentRefundedEvent(10L, 7L, 100L, null, new BigDecimal("60.00")));

        verify(invoiceUseCase, never()).reversePayment(any(), any());
    }

    @Test
    void refund_skipsWhenInvoiceUnknown() {
        when(invoiceUseCase.existsById(3L)).thenReturn(false);

        listener.onPaymentRefunded(new PaymentRefundedEvent(10L, 7L, 100L, 3L, new BigDecimal("60.00")));

        verify(invoiceUseCase, never()).reversePayment(any(), any());
    }

    /**
     * Un paiement groupé (guichet — plusieurs factures réglées en un seul geste, ex. inscription +
     * tranche 1 + tranche 2) doit solder chaque facture SANS notification individuelle : le parent
     * reçoit déjà une notification unique via PaymentSucceededEvent pour tout le règlement.
     */
    @Test
    void groupedSettlement_appliesEachShareSilently() {
        when(invoiceUseCase.existsById(3L)).thenReturn(true);
        when(invoiceUseCase.existsById(4L)).thenReturn(true);

        listener.onGroupedSettlement(new GroupedInvoiceSettlementEvent(10L, 7L, 200L, List.of(
                new GroupedInvoiceSettlementEvent.Share(3L, 7L, new BigDecimal("100.00"), null),
                new GroupedInvoiceSettlementEvent.Share(4L, 7L, new BigDecimal("60.00"), null))));

        verify(invoiceUseCase).applyPayment(3L, new BigDecimal("100.00"), false, null);
        verify(invoiceUseCase).applyPayment(4L, new BigDecimal("60.00"), false, null);
        verify(invoiceUseCase, never()).applyPayment(any(), any());
    }
}
