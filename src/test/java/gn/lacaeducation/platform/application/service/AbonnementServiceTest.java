package gn.lacaeducation.platform.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.platform.application.event.PlanChangedEvent;
import gn.lacaeducation.platform.application.event.SubscriptionActivatedEvent;
import gn.lacaeducation.platform.application.event.SubscriptionPaymentFailedEvent;
import gn.lacaeducation.platform.application.event.SubscriptionPaymentSucceededEvent;
import gn.lacaeducation.platform.application.event.SubscriptionSuspendedEvent;
import gn.lacaeducation.platform.application.port.in.PlanChangeUseCase;
import gn.lacaeducation.platform.application.port.in.SubscriptionUseCase;
import gn.lacaeducation.platform.application.port.out.PlanChangeRepository;
import gn.lacaeducation.platform.application.port.out.PlanRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionInvoiceRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionReceiptRepository;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.domain.BillingPeriod;
import gn.lacaeducation.platform.domain.InvoiceStatus;
import gn.lacaeducation.platform.domain.PaymentChannel;
import gn.lacaeducation.platform.domain.PaymentStatus;
import gn.lacaeducation.platform.domain.PaymentType;
import gn.lacaeducation.platform.domain.Plan;
import gn.lacaeducation.platform.domain.PlanChangeType;
import gn.lacaeducation.platform.domain.PlanCode;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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

        // confirm() publishes both SubscriptionActivatedEvent and SubscriptionPaymentSucceededEvent
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        SubscriptionActivatedEvent activated = eventCaptor.getAllValues().stream()
                .filter(SubscriptionActivatedEvent.class::isInstance)
                .map(SubscriptionActivatedEvent.class::cast).findFirst().orElseThrow();
        assertThat(activated.tenantId()).isEqualTo(5L);
        assertThat(activated.subscriptionId()).isEqualTo(20L);
        assertThat(eventCaptor.getAllValues()).anySatisfy(e ->
                assertThat(e).isInstanceOf(SubscriptionPaymentSucceededEvent.class));

        verify(receiptRepository).save(any());
    }

    @Test
    void initiate_archivedPlan_rejected() {
        Plan archived = Plan.builder().id(7L).name("Old").code(PlanCode.STARTER)
                .monthlyPrice(new BigDecimal("25000")).annualPrice(new BigDecimal("250000"))
                .archived(true).build();
        when(planRepository.findById(7L)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> abonnementService.initiate(
                new SubscriptionUseCase.InitiateSubscriptionCommand(5L, 7L, BillingPeriod.MONTHLY, false)))
                .isInstanceOf(BusinessRuleException.class);
        verify(subscriptionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void suspend_publishesSuspendedEvent_andTransitions() {
        Subscription sub = Subscription.builder().id(20L).tenantId(5L)
                .status(SubscriptionStatus.ACTIVE).endDate(LocalDate.now().plusMonths(1)).build();
        when(subscriptionRepository.findById(20L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        abonnementService.suspend(20L);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        verify(eventPublisher).publishEvent(any(SubscriptionSuspendedEvent.class));
    }

    @Test
    void suspend_illegalTransitionFromCancelled_throws() {
        Subscription sub = Subscription.builder().id(20L).tenantId(5L)
                .status(SubscriptionStatus.CANCELLED).endDate(LocalDate.now().plusMonths(1)).build();
        when(subscriptionRepository.findById(20L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> abonnementService.suspend(20L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void fail_publishesPaymentFailedEvent() {
        SubscriptionPayment payment = SubscriptionPayment.builder().id(1L).invoiceId(10L)
                .amount(new BigDecimal("29500")).status(PaymentStatus.PENDING).build();
        SubscriptionInvoice invoice = SubscriptionInvoice.builder().id(10L).subscriptionId(20L).build();
        Subscription sub = Subscription.builder().id(20L).tenantId(5L)
                .status(SubscriptionStatus.PENDING).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.findById(10L)).thenReturn(Optional.of(invoice));
        when(subscriptionRepository.findById(20L)).thenReturn(Optional.of(sub));

        abonnementService.fail(1L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventPublisher).publishEvent(any(SubscriptionPaymentFailedEvent.class));
    }

    @Test
    void changePlan_upgrade_computesProrataAndPublishesEvent() {
        Subscription sub = Subscription.builder().id(20L).tenantId(5L).planId(1L)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(30))
                .amount(new BigDecimal("25000")).build();
        Plan starter = Plan.builder().id(1L).code(PlanCode.STARTER)
                .monthlyPrice(new BigDecimal("25000")).annualPrice(new BigDecimal("250000")).build();
        Plan standard = Plan.builder().id(2L).code(PlanCode.STANDARD)
                .monthlyPrice(new BigDecimal("75000")).annualPrice(new BigDecimal("750000")).build();
        when(subscriptionRepository.findById(20L)).thenReturn(Optional.of(sub));
        when(planRepository.findById(1L)).thenReturn(Optional.of(starter));
        when(planRepository.findById(2L)).thenReturn(Optional.of(standard));
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<gn.lacaeducation.platform.domain.PlanChange> pc =
                ArgumentCaptor.forClass(gn.lacaeducation.platform.domain.PlanChange.class);

        abonnementService.changePlan(new PlanChangeUseCase.ChangePlanCommand(20L, 2L, BillingPeriod.MONTHLY));

        verify(planChangeRepository).save(pc.capture());
        // ~30 days left * (75000-25000) / 30 = 50000
        assertThat(pc.getValue().getProratedAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(pc.getValue().getType()).isEqualTo(PlanChangeType.UPGRADE);
        assertThat(sub.getPlanId()).isEqualTo(2L);
        verify(eventPublisher).publishEvent(any(PlanChangedEvent.class));
    }
}
