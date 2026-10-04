package gn.lacaeducation.audit.application.port.in;

import gn.lacaeducation.audit.domain.AccountantAnomalyAlert;

import java.util.List;

public interface AccountantAnomalyAlertUseCase {

    /** Réévalue les deux signaux (écarts de caisse, volume d'exonérations) pour tous les comptables
     * d'un tenant — appelé par {@code AccountantAnomalyAlertScheduler} (hebdomadaire). */
    void evaluateForTenant(Long tenantId);

    /** Vue école (SCHOOL_ADMIN/DIRECTOR) : toutes les alertes du tenant courant. */
    List<AccountantAnomalyAlert> findForCurrentTenant();
}
