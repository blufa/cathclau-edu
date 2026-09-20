package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.communication.application.event.CommunicationRecipientsPendingEvent;
import gn.lacaeducation.communication.application.port.in.SmsRetryQueueUseCase;
import gn.lacaeducation.communication.application.port.out.CommunicationRepository;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.MessageRecipient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Envoi effectif (réseau) après commit — c'est ici, pas dans CommunicationService, que le fournisseur
 * est réellement appelé, désormais en tâche de fond (voir CommunicationDispatchListener). */
@ExtendWith(MockitoExtension.class)
class CommunicationDispatchListenerTest {

    @Mock NotificationGateway smsGateway;
    @Mock MessageRecipientRepository recipientRepo;
    @Mock CommunicationRepository communicationRepo;
    @Mock SmsRetryQueueUseCase smsRetryQueueUseCase;

    @Test
    void onRecipientsPending_dispatchesEachRecipient_andRecordsDeliveredStatus() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(smsGateway), recipientRepo, communicationRepo, smsRetryQueueUseCase);

        MessageRecipient recipient = MessageRecipient.builder().id(20L).communicationId(10L)
                .destination("623273737").deliveryStatus(DeliveryStatus.PENDING).build();
        when(recipientRepo.findById(20L)).thenReturn(Optional.of(recipient));
        when(smsGateway.send("623273737", "sujet", "contenu"))
                .thenReturn(new NotificationGateway.DeliveryResult(true, "ok", "nimba-msg-1"));

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        assertThat(recipient.getDeliveryStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(recipient.getProviderMessageId()).isEqualTo("nimba-msg-1");
        assertThat(recipient.getDeliveredAt()).isNotNull();
        verify(recipientRepo).save(recipient);
        verify(smsRetryQueueUseCase, never()).enqueue(any());
    }

    @Test
    void onRecipientsPending_failedSmsDelivery_queuedForRetryInsteadOfMarkedFailed() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(smsGateway), recipientRepo, communicationRepo, smsRetryQueueUseCase);

        MessageRecipient recipient = MessageRecipient.builder().id(20L).communicationId(10L)
                .destination("623273737").deliveryStatus(DeliveryStatus.PENDING).build();
        when(recipientRepo.findById(20L)).thenReturn(Optional.of(recipient));
        when(smsGateway.send(any(), any(), any())).thenReturn(new NotificationGateway.DeliveryResult(false, "erreur fournisseur"));
        when(communicationRepo.findById(10L)).thenReturn(Optional.of(Communication.builder().id(10L).tenantId(1L).build()));

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        // Pas FAILED tout de suite : un renvoi automatique est en cours (voir SmsRetryDispatchScheduler).
        assertThat(recipient.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(recipient.getDeliveredAt()).isNull();
        verify(smsRetryQueueUseCase).enqueue(new SmsRetryQueueUseCase.EnqueueCommand(
                1L, 20L, "623273737", "sujet", "contenu", "erreur fournisseur"));
    }

    @Test
    void onRecipientsPending_noGatewayForChannel_doesNothing() {
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(), recipientRepo, communicationRepo, smsRetryQueueUseCase);

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        verify(recipientRepo, never()).findById(any());
    }

    @Test
    void onRecipientsPending_gatewayThrows_queuedForRetryInsteadOfPropagating() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(smsGateway), recipientRepo, communicationRepo, smsRetryQueueUseCase);

        MessageRecipient recipient = MessageRecipient.builder().id(20L).communicationId(10L)
                .destination("623273737").deliveryStatus(DeliveryStatus.PENDING).build();
        when(recipientRepo.findById(20L)).thenReturn(Optional.of(recipient));
        when(smsGateway.send(any(), any(), any())).thenThrow(new RuntimeException("timeout réseau"));
        when(communicationRepo.findById(10L)).thenReturn(Optional.of(Communication.builder().id(10L).tenantId(1L).build()));

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        assertThat(recipient.getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);
        verify(recipientRepo).save(recipient);
        verify(smsRetryQueueUseCase).enqueue(eq(new SmsRetryQueueUseCase.EnqueueCommand(
                1L, 20L, "623273737", "sujet", "contenu", "exception à l'envoi")));
    }
}
