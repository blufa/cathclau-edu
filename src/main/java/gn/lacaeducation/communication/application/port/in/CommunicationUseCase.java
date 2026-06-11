package gn.lacaeducation.communication.application.port.in;
import gn.lacaeducation.communication.domain.*;
import java.util.List;
import java.util.Optional;
public interface CommunicationUseCase {
    record SendCommunicationCommand(Long tenantId, Long senderId, CommunicationChannel channel,
                                    RecipientType recipientType, String subject, String content,
                                    CommunicationReason reason, List<Long> recipientUserIds) {}
    Communication send(SendCommunicationCommand command);
    Optional<Communication> findById(Long id);
    List<Communication> findByTenantId(Long tenantId);
    List<Communication> findBySenderId(Long senderId);
    List<Communication> findByTenantIdAndChannel(Long tenantId, CommunicationChannel channel);
    List<Communication> findByTenantIdAndReason(Long tenantId, CommunicationReason reason);
    void delete(Long id);
}
