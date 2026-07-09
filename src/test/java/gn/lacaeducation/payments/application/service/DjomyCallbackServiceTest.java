package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.payments.PaymentCallbackHandler;
import gn.lacaeducation.payments.application.port.out.DjomyVerificationPort;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DjomyCallbackServiceTest {

    @Mock DjomyVerificationPort verifier;
    @Mock PaymentTransactionRepository transactionRepo;
    @Mock PaymentCallbackHandler callbackHandler;
    @InjectMocks DjomyCallbackService service;

    private final PaymentTransaction tx = PaymentTransaction.builder().id(5L).paymentId(9L)
            .externalTransactionId("TX-1").build();

    @Test
    void confirmSuccess_applies_when_verify_confirms() throws Exception {
        when(transactionRepo.findByExternalTransactionId("TX-1")).thenReturn(Optional.of(tx));
        when(verifier.isConfirmedSuccess("TX-1")).thenReturn(true);

        assertThat(service.confirmSuccess("TX-1")).isTrue();
        verify(callbackHandler).applyCallback(eq(5L), eq(true), any(), any());
    }

    @Test
    void confirmSuccess_does_nothing_when_verify_contradicts_the_webhook() throws Exception {
        when(transactionRepo.findByExternalTransactionId("TX-1")).thenReturn(Optional.of(tx));
        when(verifier.isConfirmedSuccess("TX-1")).thenReturn(false);

        assertThat(service.confirmSuccess("TX-1")).isFalse();
        verify(callbackHandler, never()).applyCallback(anyLong(), anyBoolean(), any(), any());
    }

    @Test
    void confirmSuccess_false_when_transaction_unknown() throws Exception {
        when(transactionRepo.findByExternalTransactionId("TX-X")).thenReturn(Optional.empty());

        assertThat(service.confirmSuccess("TX-X")).isFalse();
        verify(verifier, never()).isConfirmedSuccess(any());
        verifyNoInteractions(callbackHandler);
    }

    @Test
    void markFailed_drives_payment_to_failed() throws Exception {
        when(transactionRepo.findByExternalTransactionId("TX-1")).thenReturn(Optional.of(tx));

        service.markFailed("TX-1");
        verify(callbackHandler).applyCallback(eq(5L), eq(false), any(), any());
    }
}
