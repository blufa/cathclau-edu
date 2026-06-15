package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.application.port.out.MessageTemplateRepository;
import gn.lacaeducation.communication.domain.CommunicationReason;
import gn.lacaeducation.communication.domain.MessageTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class MessageTemplatePersistenceAdapter implements MessageTemplateRepository {
    private final MessageTemplateJpaRepository jpa;
    private final CommunicationMapper mapper;
    @Override public MessageTemplate save(MessageTemplate t) { return mapper.toDomain(jpa.save(mapper.toEntity(t))); }
    @Override public Optional<MessageTemplate> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<MessageTemplate> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<MessageTemplate> findByTenantIdAndReason(Long t, CommunicationReason r) { return jpa.findByTenantIdAndReason(t, r).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
