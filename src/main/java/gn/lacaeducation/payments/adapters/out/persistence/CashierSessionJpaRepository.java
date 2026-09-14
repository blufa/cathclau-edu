package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.domain.CashierSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface CashierSessionJpaRepository extends JpaRepository<CashierSessionJpaEntity, Long> {
    Optional<CashierSessionJpaEntity> findByCashierIdAndStatus(Long cashierId, CashierSessionStatus status);
    List<CashierSessionJpaEntity> findByTenantIdOrderByOpenedAtDesc(Long tenantId);
    List<CashierSessionJpaEntity> findByTenantIdAndCashierIdOrderByOpenedAtDesc(Long tenantId, Long cashierId);
    List<CashierSessionJpaEntity> findByTenantIdAndStatusOrderByOpenedAtDesc(Long tenantId, CashierSessionStatus status);
    List<CashierSessionJpaEntity> findByTenantIdAndCashierIdAndStatusOrderByOpenedAtDesc(Long tenantId, Long cashierId, CashierSessionStatus status);
}
