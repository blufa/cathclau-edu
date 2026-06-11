package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionInvoiceRepository;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
class SubscriptionInvoicePersistenceAdapter implements SubscriptionInvoiceRepository {
    private final SubscriptionInvoiceJpaRepository jpa;
    private final PlatformMapper mapper;
    @Override public SubscriptionInvoice save(SubscriptionInvoice i) { return mapper.toDomain(jpa.save(mapper.toEntity(i))); }
    @Override public Optional<SubscriptionInvoice> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<SubscriptionInvoice> findLatestBySubscriptionId(Long subscriptionId) {
        return jpa.findTopBySubscriptionIdOrderByIssuedAtDesc(subscriptionId).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpa.existsById(id); }
}
