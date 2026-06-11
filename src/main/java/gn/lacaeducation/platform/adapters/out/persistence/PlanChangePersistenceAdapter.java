package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PlanChangeRepository;
import gn.lacaeducation.platform.domain.PlanChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class PlanChangePersistenceAdapter implements PlanChangeRepository {
    private final PlanChangeJpaRepository jpa;
    private final PlatformMapper mapper;
    @Override public PlanChange save(PlanChange c) { return mapper.toDomain(jpa.save(mapper.toEntity(c))); }
    @Override public Optional<PlanChange> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public List<PlanChange> findBySubscriptionId(Long id) {
        return jpa.findBySubscriptionId(id).stream().map(mapper::toDomain).toList(); }
}
