package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionReceiptRepository;
import gn.lacaeducation.platform.domain.SubscriptionReceipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
class SubscriptionReceiptPersistenceAdapter implements SubscriptionReceiptRepository {
    private final SubscriptionReceiptJpaRepository jpa;
    private final PlatformMapper mapper;
    @Override public SubscriptionReceipt save(SubscriptionReceipt r) { return mapper.toDomain(jpa.save(mapper.toEntity(r))); }
    @Override public Optional<SubscriptionReceipt> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<SubscriptionReceipt> findByPaymentId(Long paymentId) {
        return jpa.findByPaymentId(paymentId).map(mapper::toDomain); }
}
