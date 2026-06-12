package gn.lacaeducation.platform.application.port.out;
import gn.lacaeducation.platform.domain.Subscription;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface SubscriptionRepository {
    Subscription save(Subscription s);
    Optional<Subscription> findById(Long id);
    Optional<Subscription> findActiveByTenantId(Long tenantId);
    List<Subscription> findByTenantId(Long tenantId);
    List<Subscription> findAll();
    List<Subscription> findActiveExpiredBefore(LocalDate date);
    List<Subscription> findActiveExpiringOn(LocalDate date);
    void deleteById(Long id);
}
