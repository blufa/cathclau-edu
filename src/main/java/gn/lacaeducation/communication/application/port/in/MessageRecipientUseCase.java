package gn.lacaeducation.communication.application.port.in;
import gn.lacaeducation.communication.domain.*;
import java.util.List;
import java.util.Optional;
public interface MessageRecipientUseCase {
    Optional<MessageRecipient> findById(Long id);
    List<MessageRecipient> findByCommunicationId(Long communicationId);
    List<MessageRecipient> findByUserId(Long userId);
    List<MessageRecipient> findUnreadByUserId(Long userId);
    MessageRecipient markDelivered(Long id);
    MessageRecipient markFailed(Long id);
    MessageRecipient markRead(Long id);
    long countUnreadByUserId(Long userId);
}
