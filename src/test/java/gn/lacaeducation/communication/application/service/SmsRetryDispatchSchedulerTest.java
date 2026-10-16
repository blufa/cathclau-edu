package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.application.port.out.SmsRetryQueueRepository;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.MessageRecipient;
import gn.lacaeducation.communication.domain.SmsRetryEntry;
import gn.lacaeducation.communication.domain.SmsRetryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsRetryDispatchSchedulerTest {

    @Mock SmsRetryQueueRepository queueRepository;
    @Mock MessageRecipientRepository recipientRepository;
    @Mock NotificationGateway smsGateway;
    SmsRetryDispatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SmsRetryDispatchScheduler(queueRepository, recipientRepository, List.of(smsGateway));
    }

    private SmsRetryEntry entry(int attemptCount) {
        return SmsRetryEntry.builder().id(1L).tenantId(1L).recipientId(20L)
                .destination("623273737").subject("sujet").content("contenu")
                .status(SmsRetryStatus.PENDING).attemptCount(attemptCount)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1)).build();
    }

    @Test
    void noSmsGatewayConfigured_doesNothing() {
        SmsRetryDispatchScheduler noGatewayScheduler = new SmsRetryDispatchScheduler(queueRepository, recipientRepository, List.of());

        noGatewayScheduler.dispatchPendingSms();

        verify(queueRepository, never()).findByStatusAndNextAttemptAtBefore(any(), any());
    }

    @Test
    void successfulRetry_marksSentAndSyncsRecipientAsDelivered() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        SmsRetryEntry entry = entry(1);
        when(queueRepository.findByStatusAndNextAttemptAtBefore(any(), any())).thenReturn(List.of(entry));
        when(smsGateway.send("623273737", "sujet", "contenu"))
                .thenReturn(new NotificationGateway.DeliveryResult(true, "ok", "nimba-msg-2"));
        MessageRecipient recipient = MessageRecipient.builder().id(20L).deliveryStatus(DeliveryStatus.PENDING).build();
        when(recipientRepository.findById(20L)).thenReturn(Optional.of(recipient));

        scheduler.dispatchPendingSms();

        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.SENT);
        assertThat(recipient.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(recipient.getProviderMessageId()).isEqualTo("nimba-msg-2");
        assertThat(recipient.getDeliveredAt()).isNotNull();
        verify(queueRepository).save(entry);
        verify(recipientRepository).save(recipient);
    }

    @Test
    void failedRetry_schedulesBackoffAndStaysPending() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        SmsRetryEntry entry = entry(1);
        when(queueRepository.findByStatusAndNextAttemptAtBefore(any(), any())).thenReturn(List.of(entry));
        when(smsGateway.send(any(), any(), any())).thenReturn(new NotificationGateway.DeliveryResult(false, "erreur fournisseur"));

        scheduler.dispatchPendingSms();

        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        assertThat(entry.getAttemptCount()).isEqualTo(2);
        assertThat(entry.getLastError()).isEqualTo("erreur fournisseur");
        assertThat(entry.getNextAttemptAt()).isAfter(LocalDateTime.now());
        verify(recipientRepository, never()).findById(any());
        verify(queueRepository).save(entry);
    }

    @Test
    void failedRetry_manyAttempts_neverGivesUp_backoffCapped() {
        // Pas d'abandon automatique (§retour direction) : même après de très nombreuses tentatives,
        // l'entrée reste PENDING indéfiniment — seul le délai entre essais plafonne (30 minutes).
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        SmsRetryEntry entry = entry(50);
        when(queueRepository.findByStatusAndNextAttemptAtBefore(any(), any())).thenReturn(List.of(entry));
        when(smsGateway.send(any(), any(), any())).thenReturn(new NotificationGateway.DeliveryResult(false, "erreur fournisseur"));

        scheduler.dispatchPendingSms();

        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        assertThat(entry.getAttemptCount()).isEqualTo(51);
        assertThat(entry.getNextAttemptAt())
                .isAfterOrEqualTo(LocalDateTime.now().plusMinutes(29))
                .isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(31));
        verify(recipientRepository, never()).findById(any());
        verify(queueRepository).save(entry);
    }

    /**
     * Régression production : DateTimeException "Invalid value for EpochDay" — l'exposant de
     * Math.pow(2, attempts) n'était jamais plafonné avant le calcul, seul le résultat final l'était.
     * Au-delà d'environ 64 tentatives (atteint en un peu plus d'un jour au rythme plafonné de 30 min),
     * Math.pow(2, attempts) sature à Long.MAX_VALUE, puis Long.MAX_VALUE * BACKOFF_BASE_SECONDS déborde
     * silencieusement (overflow long) vers une valeur négative absurde, que plusSeconds() ne peut pas
     * représenter — l'exception faisait alors échouer toute la transaction (donc tout le lot de SMS en
     * cours, pas que cette entrée). 200 tentatives dépasse largement ce seuil, confirmant qu'il n'y a
     * plus d'exception et que le délai reste correctement plafonné à 30 min.
     */
    @Test
    void failedRetry_veryManyAttempts_neverOverflowsBackoffCalculation() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        SmsRetryEntry entry = entry(200);
        when(queueRepository.findByStatusAndNextAttemptAtBefore(any(), any())).thenReturn(List.of(entry));
        when(smsGateway.send(any(), any(), any())).thenReturn(new NotificationGateway.DeliveryResult(false, "solde insuffisant"));

        scheduler.dispatchPendingSms();

        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        assertThat(entry.getAttemptCount()).isEqualTo(201);
        assertThat(entry.getNextAttemptAt())
                .isAfterOrEqualTo(LocalDateTime.now().plusMinutes(29))
                .isBeforeOrEqualTo(LocalDateTime.now().plusMinutes(31));
        verify(queueRepository).save(entry);
    }

    @Test
    void gatewayThrows_treatedAsFailureAndRetried() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        SmsRetryEntry entry = entry(0);
        when(queueRepository.findByStatusAndNextAttemptAtBefore(any(), any())).thenReturn(List.of(entry));
        when(smsGateway.send(any(), any(), any())).thenThrow(new RuntimeException("timeout réseau"));

        scheduler.dispatchPendingSms();

        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        assertThat(entry.getAttemptCount()).isEqualTo(1);
        assertThat(entry.getLastError()).isEqualTo("timeout réseau");
    }

    @Test
    void entryWithoutRecipientId_doesNotTouchRecipientRepository() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        SmsRetryEntry entry = SmsRetryEntry.builder().id(1L).tenantId(1L).recipientId(null)
                .destination("623273737").subject("sujet").content("contenu")
                .status(SmsRetryStatus.PENDING).attemptCount(4)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1)).build();
        when(queueRepository.findByStatusAndNextAttemptAtBefore(any(), any())).thenReturn(List.of(entry));
        when(smsGateway.send(any(), any(), any())).thenReturn(new NotificationGateway.DeliveryResult(false, "erreur fournisseur"));

        scheduler.dispatchPendingSms();

        assertThat(entry.getStatus()).isEqualTo(SmsRetryStatus.PENDING);
        verify(recipientRepository, never()).findById(any());
    }
}
