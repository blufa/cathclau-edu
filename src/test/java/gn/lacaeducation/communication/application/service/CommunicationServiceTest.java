package gn.lacaeducation.communication.application.service;

import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.communication.application.event.CommunicationRecipientsPendingEvent;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.RecipientTarget;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase.SendCommunicationCommand;
import gn.lacaeducation.communication.application.port.out.CommunicationRepository;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.Communication;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.MessageRecipient;
import gn.lacaeducation.communication.domain.RecipientType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'appel réseau au fournisseur (potentiellement lent, ex. NimbaSMS) ne doit jamais se faire dans
 * send() lui-même — sinon la requête HTTP qui déclenche la notification (paiement, inscription…)
 * reste bloquée le temps de cet appel. send() ne fait que persister en PENDING et publier un
 * événement ; CommunicationDispatchListener s'occupe de l'envoi réel après commit (test séparé).
 */
@ExtendWith(MockitoExtension.class)
class CommunicationServiceTest {

    @Mock CommunicationRepository repo;
    @Mock MessageRecipientRepository recipientRepo;
    @Mock TenantInfoProvider tenantInfoProvider;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock NotificationGateway smsGateway;
    CommunicationService service;

    @Test
    void send_neverCallsGatewayDirectly_onlyPersistsPendingAndPublishesEvent() {
        when(tenantInfoProvider.findTenantInfo(1L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { Communication c = i.getArgument(0); c.setId(10L); return c; });
        when(recipientRepo.save(any())).thenAnswer(i -> { MessageRecipient r = i.getArgument(0); r.setId(20L); return r; });
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        service = new CommunicationService(repo, recipientRepo, List.of(smsGateway), tenantInfoProvider, eventPublisher);

        Communication result = service.send(new SendCommunicationCommand(1L, 5L, CommunicationChannel.SMS,
                RecipientType.PARENT, "sujet", "contenu", CommunicationReason.GENERAL,
                List.of(new RecipientTarget(null, "623273737"))));

        assertThat(result.getId()).isEqualTo(10L);
        verify(smsGateway, never()).send(any(), any(), any());

        ArgumentCaptor<MessageRecipient> savedRecipient = ArgumentCaptor.forClass(MessageRecipient.class);
        verify(recipientRepo).save(savedRecipient.capture());
        assertThat(savedRecipient.getValue().getDeliveryStatus()).isEqualTo(DeliveryStatus.PENDING);

        ArgumentCaptor<CommunicationRecipientsPendingEvent> event = ArgumentCaptor.forClass(CommunicationRecipientsPendingEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().communicationId()).isEqualTo(10L);
        assertThat(event.getValue().recipientIds()).containsExactly(20L);
    }

    @Test
    void send_recipientWithoutDestination_staysPendingAndIsNotQueuedForDispatch() {
        when(tenantInfoProvider.findTenantInfo(1L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { Communication c = i.getArgument(0); c.setId(10L); return c; });
        when(recipientRepo.save(any())).thenAnswer(i -> { MessageRecipient r = i.getArgument(0); r.setId(21L); return r; });
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        service = new CommunicationService(repo, recipientRepo, List.of(smsGateway), tenantInfoProvider, eventPublisher);

        // Destinataire sans destination résolue (juste un userId) : reste PENDING indéfiniment, pas
        // d'événement de dispatch pour lui.
        service.send(new SendCommunicationCommand(1L, 5L, CommunicationChannel.SMS,
                RecipientType.PARENT, "sujet", "contenu", CommunicationReason.GENERAL,
                List.of(new RecipientTarget(99L, null))));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void send_withIdempotencyKeyAlreadyUsed_returnsExistingCommunicationWithoutPersistingAgain() {
        Communication existing = Communication.builder().id(42L).tenantId(1L).idempotencyKey("dup-key").build();
        when(repo.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));
        service = new CommunicationService(repo, recipientRepo, List.of(smsGateway), tenantInfoProvider, eventPublisher);

        Communication result = service.send(new SendCommunicationCommand(1L, 5L, CommunicationChannel.SMS,
                RecipientType.PARENT, "sujet", "contenu", CommunicationReason.GENERAL,
                List.of(new RecipientTarget(null, "623273737")), "dup-key"));

        assertThat(result.getId()).isEqualTo(42L);
        verify(repo, never()).save(any());
        verify(recipientRepo, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void send_withNewIdempotencyKey_persistsItOnTheCommunication() {
        when(tenantInfoProvider.findTenantInfo(1L)).thenReturn(Optional.empty());
        when(repo.findByIdempotencyKey("new-key")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> { Communication c = i.getArgument(0); c.setId(11L); return c; });
        when(recipientRepo.save(any())).thenAnswer(i -> { MessageRecipient r = i.getArgument(0); r.setId(22L); return r; });
        when(smsGateway.getChannel()).thenReturn(CommunicationChannel.SMS);
        service = new CommunicationService(repo, recipientRepo, List.of(smsGateway), tenantInfoProvider, eventPublisher);

        service.send(new SendCommunicationCommand(1L, 5L, CommunicationChannel.SMS,
                RecipientType.PARENT, "sujet", "contenu", CommunicationReason.GENERAL,
                List.of(new RecipientTarget(null, "623273737")), "new-key"));

        ArgumentCaptor<Communication> saved = ArgumentCaptor.forClass(Communication.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("new-key");
    }
}
