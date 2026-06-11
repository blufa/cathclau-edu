package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {
    Optional<SubscriptionJpaEntity> findByTenantIdAndStatus(Long tenantId, SubscriptionStatus status);
}
