package gn.lacaeducation.communication.application.port.in;
import gn.lacaeducation.communication.domain.CommunicationChannel;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.MessageTemplate;
import java.util.List;
import java.util.Optional;

/** Per-tenant message templates with {@code {{variable}}} placeholders (§11.2 BONUS). */
public interface MessageTemplateUseCase {
    record SaveTemplateCommand(Long tenantId, CommunicationReason reason, CommunicationChannel channel,
                               String subject, String body) {}

    MessageTemplate create(SaveTemplateCommand command);
    MessageTemplate update(Long id, SaveTemplateCommand command);
    Optional<MessageTemplate> findById(Long id);
    List<MessageTemplate> findByTenantId(Long tenantId);
    void delete(Long id);
}
