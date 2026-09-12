package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.application.port.out.MessageRecipientRepository;
import gn.lacaeducation.communication.domain.MessageRecipient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class MessageRecipientPersistenceAdapter implements MessageRecipientRepository {
    private final MessageRecipientJpaRepository jpa;
    private final CommunicationMapper mapper;
    // saveAndFlush : même raison que CommunicationPersistenceAdapter — CommunicationService.send()
    // utilise l'id généré ici tout de suite (liste des destinataires à traiter par
    // CommunicationDispatchListener), Hibernate 6/7 ne le garantit plus disponible sans flush explicite.
    @Override public MessageRecipient save(MessageRecipient r) { return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(r))); }
    @Override public Optional<MessageRecipient> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<MessageRecipient> findByCommunicationId(Long id) { return jpa.findByCommunicationId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<MessageRecipient> findByUserId(Long id) { return jpa.findByUserId(id).stream().map(mapper::toDomain).toList(); }
    @Override public List<MessageRecipient> findByUserIdAndRead(Long id, boolean r) { return jpa.findByUserIdAndRead(id, r).stream().map(mapper::toDomain).toList(); }
    @Override public long countByUserIdAndRead(Long id, boolean r) { return jpa.countByUserIdAndRead(id, r); }
    @Override public Optional<MessageRecipient> findByProviderMessageId(String id) { return jpa.findByProviderMessageId(id).map(mapper::toDomain); }
}
