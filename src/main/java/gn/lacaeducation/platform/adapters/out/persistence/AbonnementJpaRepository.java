package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {
    Optional<SubscriptionJpaEntity> findFirstByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, SubscriptionStatus status);
    List<SubscriptionJpaEntity> findByTenantId(Long tenantId);
}
