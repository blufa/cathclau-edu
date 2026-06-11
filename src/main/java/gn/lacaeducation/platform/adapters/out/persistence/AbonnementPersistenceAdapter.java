package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionPersistenceAdapter implements SubscriptionRepository {

    private final SubscriptionJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public Subscription save(Subscription subscription) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(subscription)));
    }

    @Override
    public Optional<Subscription> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findActiveByTenantId(Long tenantId) {
        return jpaRepository.findFirstByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, SubscriptionStatus.ACTIVE)
                .map(mapper::toDomain);
    }

    @Override
    public List<Subscription> findByTenantId(Long tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
