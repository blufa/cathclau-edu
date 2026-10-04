package gn.lacaeducation.audit.application.port.out;

import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;
import gn.lacaeducation.audit.domain.AccountantAnomalyType;

import java.util.List;
import java.util.Optional;

public interface AccountantAnomalyAlertRepository {
    AccountantAnomalyAlert save(AccountantAnomalyAlert alert);

    Optional<AccountantAnomalyAlert> findByTenantIdAndCashierIdAndType(
            Long tenantId, Long cashierId, AccountantAnomalyType type);

    /** Vue école (SCHOOL_ADMIN/DIRECTOR) : toutes les alertes du tenant courant, actives et résolues. */
    List<AccountantAnomalyAlert> findByTenantId(Long tenantId);
}
