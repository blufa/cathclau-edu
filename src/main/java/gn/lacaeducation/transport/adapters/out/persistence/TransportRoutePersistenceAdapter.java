package gn.lacaeducation.transport.adapters.out.persistence;

import gn.lacaeducation.transport.adapters.out.persistence.mapper.TransportMapper;
import gn.lacaeducation.transport.application.port.out.TransportRouteRepository;
import gn.lacaeducation.transport.domain.TransportRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TransportRoutePersistenceAdapter implements TransportRouteRepository {
    private final TransportRouteJpaRepository jpa;
    private final TransportMapper mapper;

    @Override public TransportRoute save(TransportRoute r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<TransportRoute> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<TransportRoute> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
