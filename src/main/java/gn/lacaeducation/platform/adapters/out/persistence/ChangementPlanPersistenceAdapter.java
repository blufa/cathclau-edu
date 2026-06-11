package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.PlanChangeRepository;
import gn.lacaeducation.platform.domain.PlanChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PlanChangePersistenceAdapter implements PlanChangeRepository {

    private final PlanChangeJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public PlanChange save(PlanChange planChange) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(planChange)));
    }

    @Override
    public Optional<PlanChange> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PlanChange> findBySubscriptionId(Long subscriptionId) {
        return jpaRepository.findBySubscriptionId(subscriptionId).stream().map(mapper::toDomain).toList();
    }
}
