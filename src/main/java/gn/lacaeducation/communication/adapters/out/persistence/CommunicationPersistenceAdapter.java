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
    @Override public Communication save(Communication c) { return mapper.toDomain(jpa.save(mapper.toEntity(c))); }
    @Override public Optional<Communication> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<Communication> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<Communication> findBySenderId(Long s) { return jpa.findBySenderId(s).stream().map(mapper::toDomain).toList(); }
    @Override public List<Communication> findByTenantIdAndChannel(Long t, CommunicationChannel ch) { return jpa.findByTenantIdAndChannel(t, ch).stream().map(mapper::toDomain).toList(); }
    @Override public List<Communication> findByTenantIdAndReason(Long t, CommunicationReason r) { return jpa.findByTenantIdAndReason(t, r).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
