package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.event.PaymentFailedEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase.InitiatePaymentCommand;
import gn.lacaeducation.payments.application.port.in.ReceiptUseCase;
import gn.lacaeducation.payments.application.port.out.PaymentProviderGateway;
import gn.lacaeducation.payments.application.port.out.PaymentRepository;
import gn.lacaeducation.payments.application.port.out.PaymentTransactionRepository;
import gn.lacaeducation.payments.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository repo;
    @Mock PaymentTransactionRepository transactionRepo;
    @Mock ReceiptUseCase receiptUseCase;
    @Mock PaymentProviderGateway orangeGateway;
    @Mock ApplicationEventPublisher eventPublisher;

    private PaymentService service() {
        return new PaymentService(repo, transactionRepo, receiptUseCase, List.of(orangeGateway), eventPublisher);
    }

    private final InitiatePaymentCommand cmd = new InitiatePaymentCommand(
            10L, 3L, 7L, "PAY-1", new BigDecimal("100.00"), PaymentChannel.ORANGE_MONEY, PaymentType.COMPLETE);

    @Test
    void initiate_callsGateway_recordsTransaction_movesToPending() {
        when(orangeGateway.getChannel()).thenReturn(PaymentChannel.ORANGE_MONEY);
        when(orangeGateway.initiate(any(), any(), any(), any())).thenReturn(
                PaymentTransaction.builder().provider(PaymentProvider.ORANGE_MONEY).providerStatus("PENDING").build());
        when(repo.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        Payment p = service().initiate(cmd);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(transactionRepo).save(any(PaymentTransaction.class));
    }

    @Test
    void initiate_unknownChannel_throwsBusinessRule() {
        when(orangeGateway.getChannel()).thenReturn(PaymentChannel.ORANGE_MONEY);
        when(repo.save(any(Payment.class))).thenAnswer(i -> { Payment p = i.getArgument(0); p.setId(1L); return p; });

        InitiatePaymentCommand cardCmd = new InitiatePaymentCommand(
                10L, 3L, 7L, "PAY-2", BigDecimal.TEN, PaymentChannel.BANK_CARD, PaymentType.COMPLETE);

        assertThatThrownBy(() -> service().initiate(cardCmd)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void confirmSuccess_setsSucceeded_generatesReceipt_publishesEvent() {
        Payment pending = Payment.builder().id(1L).tenantId(10L).studentId(7L).invoiceId(3L)
                .amount(new BigDecimal("100.00")).status(PaymentStatus.PENDING).build();
        when(repo.findById(1L)).thenReturn(Optional.of(pending));
        when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment p = service().confirmSuccess(1L);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(p.getPaidAt()).isNotNull();
        verify(receiptUseCase).generate(1L);
        verify(eventPublisher).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    void confirmSuccess_onInitiated_isIllegalTransition() {
        Payment initiated = Payment.builder().id(1L).status(PaymentStatus.INITIATED).build();
        when(repo.findById(1L)).thenReturn(Optional.of(initiated));

        assertThatThrownBy(() -> service().confirmSuccess(1L)).isInstanceOf(BusinessRuleException.class);
        verify(receiptUseCase, never()).generate(any());
    }

    @Test
    void markFailed_publishesPaymentFailedEvent() {
        Payment pending = Payment.builder().id(1L).tenantId(10L).studentId(7L).invoiceId(3L)
                .amount(BigDecimal.TEN).status(PaymentStatus.PENDING).build();
        when(repo.findById(1L)).thenReturn(Optional.of(pending));
        when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        service().markFailed(1L);

        verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
    }

    @Test
    void markRefunded_onlyFromSucceeded_publishesRefundEvent() {
        Payment succeeded = Payment.builder().id(1L).tenantId(10L).studentId(7L).invoiceId(3L)
                .amount(BigDecimal.TEN).status(PaymentStatus.SUCCEEDED).paidAt(LocalDateTime.now()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(succeeded));
        when(repo.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment p = service().markRefunded(1L);

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(eventPublisher).publishEvent(any(PaymentRefundedEvent.class));
    }

    @Test
    void confirmSuccess_unknownPayment_throwsNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().confirmSuccess(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
