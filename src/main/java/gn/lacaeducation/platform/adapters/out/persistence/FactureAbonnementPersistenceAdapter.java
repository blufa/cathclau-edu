package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionInvoiceRepository;
import gn.lacaeducation.platform.domain.SubscriptionInvoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionInvoicePersistenceAdapter implements SubscriptionInvoiceRepository {

    private final SubscriptionInvoiceJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public SubscriptionInvoice save(SubscriptionInvoice invoice) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(invoice)));
    }

    @Override
    public Optional<SubscriptionInvoice> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionInvoice> findLatestBySubscriptionId(Long subscriptionId) {
        return jpaRepository.findFirstBySubscriptionIdOrderByIssuedAtDesc(subscriptionId)
                .map(mapper::toDomain);
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
