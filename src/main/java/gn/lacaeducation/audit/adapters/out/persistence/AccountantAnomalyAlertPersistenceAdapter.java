package gn.lacaeducation.audit.adapters.out.persistence;

import gn.lacaeducation.audit.adapters.out.persistence.mapper.AccountantAnomalyAlertMapper;
import gn.lacaeducation.audit.application.port.out.AccountantAnomalyAlertRepository;
import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;
import gn.lacaeducation.audit.domain.AccountantAnomalyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class AccountantAnomalyAlertPersistenceAdapter implements AccountantAnomalyAlertRepository {

    private final AccountantAnomalyAlertJpaRepository jpa;
    private final AccountantAnomalyAlertMapper mapper;

    @Override
    public AccountantAnomalyAlert save(AccountantAnomalyAlert alert) {
        return mapper.toDomain(jpa.save(mapper.toEntity(alert)));
    }

    @Override
    public Optional<AccountantAnomalyAlert> findByTenantIdAndCashierIdAndType(
            Long tenantId, Long cashierId, AccountantAnomalyType type) {
        return jpa.findByTenantIdAndCashierIdAndType(tenantId, cashierId, type).map(mapper::toDomain);
    }

    @Override
    public List<AccountantAnomalyAlert> findByTenantId(Long tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(mapper::toDomain).toList();
    }
}
