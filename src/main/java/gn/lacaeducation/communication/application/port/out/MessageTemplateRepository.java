package gn.lacaeducation.communication.application.port.out;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.MessageTemplate;
import java.util.List;
import java.util.Optional;
public interface MessageTemplateRepository {
    MessageTemplate save(MessageTemplate template);
    Optional<MessageTemplate> findById(Long id);
    List<MessageTemplate> findByTenantId(Long tenantId);
    Optional<MessageTemplate> findByTenantIdAndReason(Long tenantId, CommunicationReason reason);
    void deleteById(Long id);
}
