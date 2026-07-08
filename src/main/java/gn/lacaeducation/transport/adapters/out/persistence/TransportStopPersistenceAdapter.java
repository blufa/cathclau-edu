package gn.lacaeducation.transport.adapters.out.persistence;

import gn.lacaeducation.transport.adapters.out.persistence.mapper.TransportMapper;
import gn.lacaeducation.transport.application.port.out.TransportStopRepository;
import gn.lacaeducation.transport.domain.TransportStop;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TransportStopPersistenceAdapter implements TransportStopRepository {
    private final TransportStopJpaRepository jpa;
    private final TransportMapper mapper;

    @Override public TransportStop save(TransportStop s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<TransportStop> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<TransportStop> findByRouteIdOrderBySequenceAsc(Long r) { return jpa.findByRouteIdOrderBySequenceAsc(r).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
