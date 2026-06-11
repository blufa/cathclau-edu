package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionReceiptRepository;
import gn.lacaeducation.platform.domain.SubscriptionReceipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionReceiptPersistenceAdapter implements SubscriptionReceiptRepository {

    private final SubscriptionReceiptJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public SubscriptionReceipt save(SubscriptionReceipt receipt) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(receipt)));
    }

    @Override
    public Optional<SubscriptionReceipt> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionReceipt> findByPaymentId(Long paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(mapper::toDomain);
    }
}
