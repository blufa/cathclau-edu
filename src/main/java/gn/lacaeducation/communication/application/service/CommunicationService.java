package gn.lacaeducation.communication.application.service;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.out.CommunicationRepository;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.application.port.out.NotificationGateway;
import gn.lacaeducation.communication.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class CommunicationService implements CommunicationUseCase {
    private final CommunicationRepository repo;
    private final MessageRecipientRepository recipientRepo;
    private final List<NotificationGateway> gateways;

    @Override public Communication send(SendCommunicationCommand c) {
        LocalDateTime now = LocalDateTime.now();
        Communication comm = repo.save(Communication.builder().tenantId(c.tenantId()).senderId(c.senderId())
                .channel(c.channel()).recipientType(c.recipientType()).subject(c.subject())
                .content(c.content()).reason(c.reason()).sentAt(now).build());
        NotificationGateway gateway = gatewayFor(c.channel());
        if (c.recipients() != null) {
            for (RecipientTarget rt : c.recipients()) {
                DeliveryStatus status = DeliveryStatus.PENDING;
                LocalDateTime deliveredAt = null;
                String providerMessageId = null;
                // A destination-less recipient (manual send to a user account) stays PENDING until a
                // contact is resolved; otherwise dispatch through the channel's gateway and record the outcome.
                if (gateway != null && rt.destination() != null && !rt.destination().isBlank()) {
                    NotificationGateway.DeliveryResult res = gateway.send(rt.destination(), c.subject(), c.content());
                    status = res.delivered() ? DeliveryStatus.DELIVERED : DeliveryStatus.FAILED;
                    deliveredAt = res.delivered() ? now : null;
                    providerMessageId = res.providerMessageId();
                }
                recipientRepo.save(MessageRecipient.builder().communicationId(comm.getId())
                        .userId(rt.userId()).destination(rt.destination())
                        .deliveryStatus(status).read(false).deliveredAt(deliveredAt)
                        .providerMessageId(providerMessageId).build());
            }
        }
        return comm;
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
