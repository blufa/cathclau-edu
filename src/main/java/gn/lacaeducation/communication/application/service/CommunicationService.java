package gn.lacaeducation.communication.application.service;
import gn.lacaeducation.communication.application.port.in.CommunicationUseCase;
import gn.lacaeducation.communication.application.port.out.CommunicationRepository;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
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
    @Override public Communication send(SendCommunicationCommand c) {
        Communication comm = repo.save(Communication.builder().tenantId(c.tenantId()).senderId(c.senderId())
                .channel(c.channel()).recipientType(c.recipientType()).subject(c.subject())
                .content(c.content()).reason(c.reason()).sentAt(LocalDateTime.now()).build());
        if (c.recipientUserIds() != null) {
            for (Long userId : c.recipientUserIds()) {
                recipientRepo.save(MessageRecipient.builder().communicationId(comm.getId())
                        .userId(userId).deliveryStatus(DeliveryStatus.PENDING).read(false).build());
            }
        }
        return comm;
    }
    @Override @Transactional(readOnly=true) public Optional<Communication> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<Communication> findByTenantId(Long t) { return repo.findByTenantId(t); }
    @Override @Transactional(readOnly=true) public List<Communication> findBySenderId(Long s) { return repo.findBySenderId(s); }
    @Override @Transactional(readOnly=true) public List<Communication> findByTenantIdAndChannel(Long t, CommunicationChannel ch) { return repo.findByTenantIdAndChannel(t, ch); }
    @Override @Transactional(readOnly=true) public List<Communication> findByTenantIdAndReason(Long t, CommunicationReason r) { return repo.findByTenantIdAndReason(t, r); }
    @Override public void delete(Long id) { repo.deleteById(id); }
}
