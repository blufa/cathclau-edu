package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.PaymentProvider;
import gn.lacaeducation.payments.domain.PaymentTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackServiceTest {

    @Mock PaymentTransactionRepository transactionRepo;
    @Mock PaymentUseCase paymentUseCase;
    @InjectMocks PaymentCallbackService service;

    private PaymentTransaction tx() {
        return PaymentTransaction.builder().id(9L).paymentId(3L).provider(PaymentProvider.ORANGE_MONEY).build();
    }

    @Test
    void success_updatesTransactionAndConfirmsPayment() {
        when(transactionRepo.findById(9L)).thenReturn(Optional.of(tx()));

        service.applyCallback(9L, true, "SUCCESS", "{\"raw\":1}");

        ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(transactionRepo).save(saved.capture());
        assertThat(saved.getValue().getProviderStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getValue().getResponsePayload()).isEqualTo("{\"raw\":1}");
        verify(paymentUseCase).confirmSuccess(3L);
        verify(paymentUseCase, never()).markFailed(any());
    }

    @Test
    void failure_marksPaymentFailed() {
        when(transactionRepo.findById(9L)).thenReturn(Optional.of(tx()));

        service.applyCallback(9L, false, "DECLINED", "{}");

        verify(paymentUseCase).markFailed(3L);
        verify(paymentUseCase, never()).confirmSuccess(any());
    }

    @Test
    void unknownTransaction_throwsNotFound() {
        when(transactionRepo.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyCallback(9L, true, "SUCCESS", "{}"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
