package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.application.port.out.CashierSessionRepository;
import gn.lacaeducation.payments.domain.CashierSession;
import gn.lacaeducation.payments.domain.CashierSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component @RequiredArgsConstructor
class CashierSessionPersistenceAdapter implements CashierSessionRepository {
    private final CashierSessionJpaRepository jpa;
    private final PaymentsMapper mapper;

    @Override public CashierSession save(CashierSession s) { return mapper.toDomain(jpa.save(mapper.toEntity(s))); }
    @Override public Optional<CashierSession> findById(Long id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<CashierSession> findOpenByCashierId(Long cashierId) {
        return jpa.findByCashierIdAndStatus(cashierId, CashierSessionStatus.OPEN).map(mapper::toDomain);
    }

    @Override public List<CashierSession> findByTenantId(Long tenantId, Long cashierId, CashierSessionStatus status) {
        List<CashierSessionJpaEntity> entities;
        if (cashierId != null && status != null) {
            entities = jpa.findByTenantIdAndCashierIdAndStatusOrderByOpenedAtDesc(tenantId, cashierId, status);
        } else if (cashierId != null) {
            entities = jpa.findByTenantIdAndCashierIdOrderByOpenedAtDesc(tenantId, cashierId);
        } else if (status != null) {
            entities = jpa.findByTenantIdAndStatusOrderByOpenedAtDesc(tenantId, status);
        } else {
            entities = jpa.findByTenantIdOrderByOpenedAtDesc(tenantId);
        }
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override public void deleteById(Long id) { jpa.deleteById(id); }
}
