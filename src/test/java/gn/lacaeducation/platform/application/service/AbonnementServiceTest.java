package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.platform.application.event.SubscriptionActivatedEvent;
import gn.lacaeducation.platform.application.port.out.PlanChangeRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionInvoiceRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionReceiptRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.domain.InvoiceStatus;
import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.PaymentType;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbonnementServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock SubscriptionInvoiceRepository invoiceRepository;
    @Mock SubscriptionPaymentRepository paymentRepository;
    @Mock SubscriptionReceiptRepository receiptRepository;
    @Mock PlanRepository planRepository;
    @Mock PlanChangeRepository planChangeRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks
    AbonnementService abonnementService;

    @Test
    void confirmPayment_successful_activatesSubscriptionAndPublishesEvent() {
        SubscriptionPayment payment = SubscriptionPayment.builder()
                .id(1L).invoiceId(10L)
                .amount(new BigDecimal("29500.00"))
                .channel(PaymentChannel.ORANGE_MONEY)
                .paymentType(PaymentType.FULL)
                .status(PaymentStatus.PENDING)
                .paidAt(LocalDateTime.now())
                .build();

        SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                .id(10L).subscriptionId(20L)
                .totalAmount(new BigDecimal("29500.00"))
                .paidAmount(BigDecimal.ZERO)
                .remainingBalance(new BigDecimal("29500.00"))
                .status(InvoiceStatus.ISSUED)
                .issuedAt(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        Subscription subscription = Subscription.builder()
                .id(20L).tenantId(5L)
                .status(SubscriptionStatus.PENDING)
                .endDate(LocalDate.now().plusMonths(1))
                .build();

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(subscriptionRepository.findById(20L)).thenReturn(Optional.of(subscription));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(receiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        abonnementService.confirm(1L, "TXN-ABC123");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        ArgumentCaptor<SubscriptionActivatedEvent> eventCaptor = ArgumentCaptor.forClass(SubscriptionActivatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().tenantId()).isEqualTo(5L);
        assertThat(eventCaptor.getValue().subscriptionId()).isEqualTo(20L);

        verify(receiptRepository).save(any());
    }
}
