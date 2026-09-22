package gn.lacaeducation.communication.application.service;
import gn.lacaeducation.common.TenantInfoProvider;
import gn.lacaeducation.common.TenantPublicInfo;
import gn.lacaeducation.communication.application.event.CommunicationRecipientsPendingEvent;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.out.CommunicationRepository;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor @Slf4j
public class CommunicationService implements CommunicationUseCase {
    private final CommunicationRepository repo;
    private final MessageRecipientRepository recipientRepo;
    private final List<NotificationGateway> gateways;
    private final TenantInfoProvider tenantInfoProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Override public Communication send(SendCommunicationCommand c) {
        // Rejoué avec la même clé (double-clic, requête réseau dupliquée) : renvoie la communication
        // déjà créée au lieu d'en créer une seconde — même pattern que PaymentService.initiateGroup().
        if (c.idempotencyKey() != null && !c.idempotencyKey().isBlank()) {
            Optional<Communication> existing = repo.findByIdempotencyKey(c.idempotencyKey());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        LocalDateTime now = LocalDateTime.now();
        // Préfixe systématique du nom de l'école sur TOUS les SMS, quel que soit l'appelant (notification
        // automatique déclenchée par un événement, diffusion manuelle composée par le personnel, ou tout
        // futur appel direct à send()) — point d'envoi unique, donc le seul endroit fiable pour garantir
        // que le destinataire identifie toujours l'expéditeur, sans dépendre de chaque appelant pour y
        // penser individuellement.
        String content = withSchoolPrefix(c.tenantId(), c.channel(), c.content());
        Communication comm = repo.save(Communication.builder().tenantId(c.tenantId()).senderId(c.senderId())
                .channel(c.channel()).recipientType(c.recipientType()).subject(c.subject())
                .content(content).reason(c.reason()).sentAt(now)
                .idempotencyKey(c.idempotencyKey() != null && !c.idempotencyKey().isBlank() ? c.idempotencyKey() : null)
                .build());
        log.info("Communication id={} tenantId={} channel={} reason={} enregistrée",
                comm.getId(), comm.getTenantId(), comm.getChannel(), comm.getReason());
        NotificationGateway gateway = gatewayFor(c.channel());
        if (c.recipients() != null) {
            List<Long> pendingRecipientIds = new ArrayList<>();
            for (RecipientTarget rt : c.recipients()) {
                MessageRecipient recipient = recipientRepo.save(MessageRecipient.builder().communicationId(comm.getId())
                        .userId(rt.userId()).destination(rt.destination())
                        .deliveryStatus(DeliveryStatus.PENDING).read(false).build());
                // A destination-less recipient (manual send to a user account) stays PENDING until a
                // contact is resolved; otherwise queued for dispatch through the channel's gateway.
                if (rt.destination() != null && !rt.destination().isBlank()) {
                    pendingRecipientIds.add(recipient.getId());
                }
            }
            // Envoi effectif différé après commit (voir CommunicationDispatchListener) : l'appel réseau
            // au fournisseur (SMS notamment) ne doit jamais bloquer la requête qui a déclenché cette
            // notification (paiement, inscription…).
            if (gateway != null && !pendingRecipientIds.isEmpty()) {
                eventPublisher.publishEvent(new CommunicationRecipientsPendingEvent(
                        comm.getId(), c.channel(), c.subject(), content, pendingRecipientIds));
                log.info("Communication id={} : dispatch programmé vers {} destinataire(s) via gateway {}",
                        comm.getId(), pendingRecipientIds.size(), gateway.getClass().getSimpleName());
            } else {
                // Silence total avant ce correctif : le message restait PENDING pour toujours sans
                // qu'aucune trace n'indique pourquoi (aucun gateway pour ce canal, ou aucun destinataire
                // avec une destination renseignée) — impossible à diagnostiquer depuis les logs seuls.
                log.warn("Communication id={} : AUCUN dispatch programmé (gateway trouvé={}, destinataires avec contact={}/{})",
                        comm.getId(), gateway != null, pendingRecipientIds.size(), c.recipients().size());
            }
        }
        return comm;
    }

    private String withSchoolPrefix(Long tenantId, CommunicationChannel channel, String content) {
        if (channel != CommunicationChannel.SMS || content == null || content.isBlank() || tenantId == null) return content;
        String school = tenantInfoProvider.findTenantInfo(tenantId).map(TenantPublicInfo::schoolName).orElse(null);
        if (school == null || school.isBlank() || content.contains(school)) return content;
        return "[" + school + "] " + content;
    }

    private NotificationGateway gatewayFor(CommunicationChannel channel) {
        return gateways.stream().filter(g -> g.getChannel() == channel).findFirst().orElse(null);
    }

    @Override @Transactional(readOnly=true) public Optional<Communication> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Communication> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Communication> findBySenderId(Long s) { return repo.findBySenderId(s); }
    @Override @Transactional(readOnly=true) public List<Communication> findByTenantIdAndChannel(Long t, CommunicationChannel ch) { return repo.findByTenantIdAndChannel(t, ch); }
    @Override @Transactional(readOnly=true) public List<Communication> findByTenantIdAndReason(Long t, CommunicationReason r) { return repo.findByTenantIdAndReason(t, r); }
    @Override public void delete(Long id) { repo.deleteById(id); }
}
