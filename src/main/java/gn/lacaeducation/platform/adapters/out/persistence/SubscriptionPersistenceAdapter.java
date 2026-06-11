package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionRepository;
import gn.lacaeducation.platform.domain.Subscription;
import gn.lacaeducation.platform.domain.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Component @RequiredArgsConstructor
class SubscriptionPersistenceAdapter implements SubscriptionRepository {
    private final SubscriptionJpaRepository jpa;
    private final PlatformMapper mapper;
    @Override public Subscription save(Subscription s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<Subscription> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<Subscription> findActiveByTenantId(Long tenantId) {
        return jpa.findByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE).map(mapper::toDomain); }
    @Override public List<Subscription> findAll() { return jpa.findAll().stream().map(mapper::toDomain).toList(); }
    @Override public List<Subscription> findActiveExpiredBefore(LocalDate date) {
        return jpa.findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, date).stream().map(mapper::toDomain).toList(); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
