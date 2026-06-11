package gn.lacaeducation.communication.application.service;
import gn.lacaeducation.communication.application.port.in.MessageRecipientUseCase;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.domain.DeliveryStatus;
import gn.lacaeducation.communication.domain.MessageRecipient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Service @Transactional @RequiredArgsConstructor
public class MessageRecipientService implements MessageRecipientUseCase {
    private final MessageRecipientRepository repo;
    @Override @Transactional(readOnly=true) public Optional<MessageRecipient> findById(Long id) { return repo.findById(id); }
    @Override @Transactional(readOnly=true) public List<MessageRecipient> findByCommunicationId(Long id) { return repo.findByCommunicationId(id); }
    @Override @Transactional(readOnly=true) public List<MessageRecipient> findByUserId(Long id) { return repo.findByUserId(id); }
    @Override @Transactional(readOnly=true) public List<MessageRecipient> findUnreadByUserId(Long id) { return repo.findByUserIdAndRead(id, false); }
    @Override public MessageRecipient markDelivered(Long id) {
        MessageRecipient r = load(id); r.setDeliveryStatus(DeliveryStatus.DELIVERED); r.setDeliveredAt(LocalDateTime.now()); return repo.save(r);
    }
    @Override public MessageRecipient markFailed(Long id) {
        MessageRecipient r = load(id); r.setDeliveryStatus(DeliveryStatus.FAILED); return repo.save(r);
    }
    @Override public MessageRecipient markRead(Long id) {
        MessageRecipient r = load(id); r.setRead(true); return repo.save(r);
    }
    @Override @Transactional(readOnly=true) public long countUnreadByUserId(Long id) { return repo.countByUserIdAndRead(id, false); }
    private MessageRecipient load(Long id) { return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("MessageRecipient not found: " + id)); }
}
