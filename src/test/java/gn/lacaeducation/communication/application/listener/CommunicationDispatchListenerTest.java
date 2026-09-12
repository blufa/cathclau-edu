package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.communication.application.event.CommunicationRecipientsPendingEvent;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Envoi effectif (réseau) après commit — c'est ici, pas dans CommunicationService, que le fournisseur
 * est réellement appelé, désormais en tâche de fond (voir CommunicationDispatchListener). */
@ExtendWith(MockitoExtension.class)
class CommunicationDispatchListenerTest {

    @Mock NotificationGateway smsGateway;
    @Mock MessageRecipientRepository recipientRepo;

    @Test
    void onRecipientsPending_dispatchesEachRecipient_andRecordsDeliveredStatus() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(smsGateway), recipientRepo);

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
    }

    @Test
    void onRecipientsPending_failedDelivery_recordsFailedStatus() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(smsGateway), recipientRepo);

        MessageRecipient recipient = MessageRecipient.builder().id(20L).communicationId(10L)
                .destination("623273737").deliveryStatus(DeliveryStatus.PENDING).build();
        when(recipientRepo.findById(20L)).thenReturn(Optional.of(recipient));
        when(smsGateway.send(any(), any(), any())).thenReturn(new NotificationGateway.DeliveryResult(false, "erreur fournisseur"));

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        assertThat(recipient.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(recipient.getDeliveredAt()).isNull();
    }

    @Test
    void onRecipientsPending_noGatewayForChannel_doesNothing() {
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(), recipientRepo);

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        verify(recipientRepo, never()).findById(any());
    }

    @Test
    void onRecipientsPending_gatewayThrows_recordsFailedStatusInsteadOfPropagating() {
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        CommunicationDispatchListener listener = new CommunicationDispatchListener(List.of(smsGateway), recipientRepo);

        MessageRecipient recipient = MessageRecipient.builder().id(20L).communicationId(10L)
                .destination("623273737").deliveryStatus(DeliveryStatus.PENDING).build();
        when(recipientRepo.findById(20L)).thenReturn(Optional.of(recipient));
        when(smsGateway.send(any(), any(), any())).thenThrow(new RuntimeException("timeout réseau"));

        listener.onRecipientsPending(new CommunicationRecipientsPendingEvent(10L, CommunicationChannel.SMS, "sujet", "contenu", List.of(20L)));

        assertThat(recipient.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        verify(recipientRepo).save(recipient);
    }
}
