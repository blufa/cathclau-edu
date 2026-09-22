package gn.lacaeducation.communication.adapters.out.persistence;
import gn.lacaeducation.communication.application.port.out.CommunicationRepository;
import gn.lacaeducation.communication.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class CommunicationPersistenceAdapter implements CommunicationRepository {
    private final CommunicationJpaRepository jpa;
    private final CommunicationMapper mapper;
    // saveAndFlush (pas save) : Hibernate 6/7 retarde par défaut l'INSERT d'une entité à génération
    // IDENTITY pour permettre le batching JDBC — sans flush immédiat, l'id généré reste null tant que
    // la transaction n'a pas flush/commit, alors que l'appelant en a besoin tout de suite (lier les
    // MessageRecipient à cette Communication, publier CommunicationRecipientsPendingEvent). Sans ce
    // flush, la notification restait indéfiniment invisible (id=null) sans jamais planter.
    @Override public Communication save(Communication c) { return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(c))); }
    @Override public Optional<Communication> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Communication> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<Communication> findBySenderId(Long s) { return jpa.findBySenderId(s).stream().map(mapper::toDomain).toList(); }
    @Override public List<Communication> findByTenantIdAndChannel(Long t, CommunicationChannel ch) { return jpa.findByTenantIdAndChannel(t, ch).stream().map(mapper::toDomain).toList(); }
    @Override public List<Communication> findByTenantIdAndReason(Long t, CommunicationReason r) { return jpa.findByTenantIdAndReason(t, r).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<Communication> findByIdempotencyKey(String key) { return jpa.findByIdempotencyKey(key).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
