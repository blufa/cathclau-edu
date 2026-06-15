package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoicePaymentListenerTest {

    @Mock InvoiceUseCase invoiceUseCase;
    @InjectMocks InvoicePaymentListener listener;

    @Test
    void appliesPaymentToInvoiceOnSuccess() {
        when(invoiceUseCase.existsById(3L)).thenReturn(true);

        listener.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, 3L, new BigDecimal("60.00")));

        verify(invoiceUseCase).applyPayment(3L, new BigDecimal("60.00"));
    }

    @Test
    void skipsWhenInvoiceIdMissing() {
        listener.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, null, new BigDecimal("60.00")));

        verify(invoiceUseCase, never()).applyPayment(any(), any());
    }

    @Test
    void skipsWhenInvoiceUnknown() {
        when(invoiceUseCase.existsById(3L)).thenReturn(false);

        listener.onPaymentSucceeded(new PaymentSucceededEvent(10L, 7L, 100L, 3L, new BigDecimal("60.00")));

        verify(invoiceUseCase, never()).applyPayment(any(), any());
    }
}
