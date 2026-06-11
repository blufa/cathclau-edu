package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.Subscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(Long id);
    Optional<Subscription> findActiveByTenantId(Long tenantId);
    List<Subscription> findByTenantId(Long tenantId);
    void deleteById(Long id);
    boolean existsById(Long id);
}
