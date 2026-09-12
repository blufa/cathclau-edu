package gn.lacaeducation.communication.application.listener;

import gn.lacaeducation.communication.application.event.CommunicationRecipientsPendingEvent;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.MessageRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Envoi effectif d'une communication (appel réseau au fournisseur) en tâche de fond, après commit de
 * la transaction qui l'a créée — jamais avant, pour être sûr que les lignes {@link MessageRecipient}
 * PENDING existent déjà en base quand ce listener les relit/met à jour (sans cette garantie, {@code
 * @Async} seul créerait une course : le thread d'arrière-plan pourrait s'exécuter avant que la
 * transaction d'origine ait validé son écriture). Même principe que {@code EmailService#send}
 * (déjà {@code @Async}), étendu au SMS/WhatsApp qui bloquaient jusqu'ici la requête HTTP (paiement,
 * inscription…) le temps de l'appel réseau au fournisseur (ex. NimbaSMS).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommunicationDispatchListener {

    private final List<NotificationGateway> gateways;
    private final MessageRecipientRepository recipientRepo;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRecipientsPending(CommunicationRecipientsPendingEvent event) {
        NotificationGateway gateway = gateways.stream().filter(g -> g.getChannel() == event.channel()).findFirst().orElse(null);
        if (gateway == null) return;
        for (Long recipientId : event.recipientIds()) {
            recipientRepo.findById(recipientId).ifPresent(recipient -> dispatchOne(gateway, recipient, event));
        }
    }

    private void dispatchOne(NotificationGateway gateway, MessageRecipient recipient, CommunicationRecipientsPendingEvent event) {
        if (recipient.getDestination() == null || recipient.getDestination().isBlank()) return;
        try {
            NotificationGateway.DeliveryResult res = gateway.send(recipient.getDestination(), event.subject(), event.content());
            recipient.setDeliveryStatus(res.delivered() ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED);
            recipient.setDeliveredAt(res.delivered() ? LocalDateTime.now() : null);
            recipient.setProviderMessageId(res.providerMessageId());
        } catch (RuntimeException e) {
            log.error("Échec envoi communication id={} vers {} : {}", event.communicationId(), recipient.getDestination(), e.getMessage());
            recipient.setDeliveryStatus(DeliveryStatus.FAILED);
        }
        recipientRepo.save(recipient);
    }
}
