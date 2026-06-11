package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.adapters.out.persistence.mapper.PlatformMapper;
import gn.lacaeducation.platform.application.port.out.SubscriptionPaymentRepository;
import gn.lacaeducation.platform.domain.SubscriptionPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionPaymentPersistenceAdapter implements SubscriptionPaymentRepository {

    private final SubscriptionPaymentJpaRepository jpaRepository;
    private final PlatformMapper mapper;

    @Override
    public SubscriptionPayment save(SubscriptionPayment payment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(payment)));
    }

    @Override
    public Optional<SubscriptionPayment> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
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
