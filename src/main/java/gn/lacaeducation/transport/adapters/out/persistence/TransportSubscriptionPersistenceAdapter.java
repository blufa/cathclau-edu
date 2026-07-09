package gn.lacaeducation.transport.adapters.out.persistence;

import gn.lacaeducation.transport.adapters.out.persistence.mapper.TransportMapper;
import gn.lacaeducation.transport.application.port.out.TransportSubscriptionRepository;
import gn.lacaeducation.transport.domain.TransportSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class TransportSubscriptionPersistenceAdapter implements TransportSubscriptionRepository {
    private final TransportSubscriptionJpaRepository jpa;
    private final TransportMapper mapper;

    @Override public TransportSubscription save(TransportSubscription s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<TransportSubscription> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<TransportSubscription> findByTenantId(Long t) { return jpa.findByTenantId(t).stream().map(mapper::toDomain).toList(); }
    @Override public List<TransportSubscription> findByRouteId(Long r) { return jpa.findByRouteId(r).stream().map(mapper::toDomain).toList(); }
    @Override public List<TransportSubscription> findByStudentId(Long s) { return jpa.findByStudentId(s).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
