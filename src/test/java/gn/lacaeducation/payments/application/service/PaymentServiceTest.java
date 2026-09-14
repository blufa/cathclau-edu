package gn.lacaeducation.payments.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.payments.application.event.PaymentFailedEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase.GroupPaymentCommand;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase.InitiatePaymentCommand;
import gn.lacaeducation.payments.application.port.in.PaymentUseCase.InvoiceShare;
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
    @Mock gn.lacaeducation.payments.application.port.out.DjomyVerificationPort djomyVerifier;

    private PaymentService service() {
        return new PaymentService(repo, transactionRepo, receiptUseCase, List.of(orangeGateway), eventPublisher, djomyVerifier,
                tools.jackson.databind.json.JsonMapper.builder().build());
    }

    private final InitiatePaymentCommand cmd = new InitiatePaymentCommand(
            10L, 3L, 7L, "PAY-1", new BigDecimal("100.00"), PaymentChannel.ORANGE_MONEY, PaymentType.COMPLETE, null, null, 99L);

    @Test
    void initiate_deskManualChannel_confirmsImmediately_generatesReceipt() throws Exception {
        // Orange Money est encaissé au guichet (isDeskManual) : l'argent est déjà reçu → confirmation
        // immédiate (SUCCEEDED), reçu émis et événement publié, sans passerelle en ligne.
        when(repo.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        Payment p = service().initiate(cmd); // cmd = ORANGE_MONEY

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(receiptUseCase).generate(1L);
        verify(eventPublisher).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    void initiate_onlineChannelWithoutGateway_throwsBusinessRule() {
        when(orangeGateway.getChannel()).thenReturn(PaymentChannel.ORANGE_MONEY);
        when(repo.save(any(Payment.class))).thenAnswer(i -> { Payment p = i.getArgument(0); p.setId(1L); return p; });

        // MTN Money est un canal en ligne (non desk-manual) : sans passerelle configurée → règle métier.
        InitiatePaymentCommand onlineCmd = new InitiatePaymentCommand(
                10L, 3L, 7L, "PAY-2", BigDecimal.TEN, PaymentChannel.MTN_MONEY, PaymentType.COMPLETE, null, null, 99L);

        assertThatThrownBy(() -> service().initiate(onlineCmd)).isInstanceOf(BusinessRuleException.class);
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

    private GroupPaymentCommand groupCmd(String idempotencyKey) {
        return new GroupPaymentCommand(10L, 7L, PaymentChannel.CASH, null, "Tranche 1",
                1, BigDecimal.ZERO, List.of(new InvoiceShare(3L, null, new BigDecimal("100.00"), null)),
                99L, 2026L, idempotencyKey);
    }

    @Test
    void initiateGroup_deskManual_confirmsImmediately_generatesReceipt() {
        // CASH est encaissé au guichet (isDeskManual) : confirmation immédiate (SUCCEEDED), reçu groupé
        // émis et événements publiés, sans passerelle en ligne (§payments — flux d'encaissement réel).
        when(repo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(repo.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        Payment p = service().initiateGroup(groupCmd("key-1"));

        assertThat(p.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(p.getIdempotencyKey()).isEqualTo("key-1");
        verify(receiptUseCase).generateGrouped(eq(1L), any(), any(), any());
        verify(eventPublisher).publishEvent(any(PaymentSucceededEvent.class));
    }

    @Test
    void initiateGroup_calledTwiceWithSameIdempotencyKey_returnsSamePayment_doesNotDuplicate() {
        // Rejoué après une coupure réseau (mode hors-ligne, §payments) avec la même clé : le second
        // appel doit renvoyer le paiement déjà créé, sans en créer un second ni régénérer le reçu.
        Payment alreadyCreated = Payment.builder().id(1L).tenantId(10L).studentId(7L)
                .amount(new BigDecimal("100.00")).status(PaymentStatus.SUCCEEDED)
                .idempotencyKey("key-1").build();
        when(repo.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(alreadyCreated));
        when(repo.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(1L);
            return p;
        });

        Payment first = service().initiateGroup(groupCmd("key-1"));
        Payment second = service().initiateGroup(groupCmd("key-1"));

        assertThat(first.getId()).isEqualTo(second.getId());
        // Le 1er appel enregistre le paiement deux fois (création INITIATED puis confirmation SUCCEEDED,
        // comportement normal d'un encaissement guichet) ; le 2nd appel, court-circuité par la clé
        // d'idempotence, ne doit déclencher AUCUNE écriture supplémentaire ni régénérer le reçu.
        verify(repo, times(2)).save(any(Payment.class));
        verify(receiptUseCase, times(1)).generateGrouped(any(), any(), any(), any());
    }

    @Test
    void dailyCollectionsReport_groupsByDay_acrossChannels_ignoresNonSucceeded() {
        // Deux paiements le même jour (canaux différents), un paiement un autre jour, un paiement FAILED
        // (ne doit pas compter) — le fondateur doit voir un total par jour, tous canaux confondus.
        LocalDateTime day1 = LocalDateTime.of(2026, 9, 1, 9, 0);
        LocalDateTime day1Later = LocalDateTime.of(2026, 9, 1, 15, 30);
        LocalDateTime day2 = LocalDateTime.of(2026, 9, 2, 10, 0);
        Payment cashDay1 = Payment.builder().id(1L).tenantId(10L).studentId(7L).channel(PaymentChannel.CASH)
                .amount(new BigDecimal("50000")).status(PaymentStatus.SUCCEEDED).paidAt(day1).build();
        Payment mobileDay1 = Payment.builder().id(2L).tenantId(10L).studentId(8L).channel(PaymentChannel.ORANGE_MONEY)
                .amount(new BigDecimal("30000")).status(PaymentStatus.SUCCEEDED).paidAt(day1Later).build();
        Payment cashDay2 = Payment.builder().id(3L).tenantId(10L).studentId(9L).channel(PaymentChannel.CASH)
                .amount(new BigDecimal("20000")).status(PaymentStatus.SUCCEEDED).paidAt(day2).build();
        Payment failedDay1 = Payment.builder().id(4L).tenantId(10L).studentId(11L).channel(PaymentChannel.CASH)
                .amount(new BigDecimal("99999")).status(PaymentStatus.FAILED).paidAt(day1).build();
        when(repo.findByPaidAtBetween(any(), any()))
                .thenReturn(List.of(cashDay1, mobileDay1, cashDay2, failedDay1));

        var report = service().dailyCollectionsReport(day1.minusDays(1), day2.plusDays(1));

        assertThat(report.grandTotal()).isEqualByComparingTo("100000");
        assertThat(report.days()).hasSize(2);
        assertThat(report.days().get(0).date()).isEqualTo(java.time.LocalDate.of(2026, 9, 1));
        assertThat(report.days().get(0).total()).isEqualByComparingTo("80000");
        assertThat(report.days().get(0).count()).isEqualTo(2);
        assertThat(report.days().get(1).date()).isEqualTo(java.time.LocalDate.of(2026, 9, 2));
        assertThat(report.days().get(1).total()).isEqualByComparingTo("20000");
    }
}
