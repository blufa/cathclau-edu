package gn.lacaeducation.platform.adapters.out.persistence;
import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
@Component @RequiredArgsConstructor
class SubscriptionPaymentPersistenceAdapter implements SubscriptionPaymentRepository {
    private final SubscriptionPaymentJpaRepository jpa;
    private final PlatformMapper mapper;
    @Override public SubscriptionPayment save(SubscriptionPayment p) { return mapper.toDomain(jpa.save(mapper.toEntity(p))); }
    @Override public Optional<SubscriptionPayment> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public void deleteById(Long id) { jpa.deleteById(id); }
    @Override public boolean existsById(Long id) { return jpa.existsById(id); }
}
