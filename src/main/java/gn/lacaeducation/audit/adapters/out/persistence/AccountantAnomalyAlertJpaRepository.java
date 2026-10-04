package gn.lacaeducation.audit.adapters.out.persistence;

import gn.lacaeducation.audit.domain.AccountantAnomalyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface AccountantAnomalyAlertJpaRepository extends JpaRepository<AccountantAnomalyAlertJpaEntity, Long> {
    Optional<AccountantAnomalyAlertJpaEntity> findByTenantIdAndCashierIdAndType(
            Long tenantId, Long cashierId, AccountantAnomalyType type);

    List<AccountantAnomalyAlertJpaEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
