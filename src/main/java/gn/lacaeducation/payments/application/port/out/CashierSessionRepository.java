package gn.lacaeducation.payments.application.port.out;

import gn.lacaeducation.payments.domain.CashierSession;
import gn.lacaeducation.payments.domain.CashierSessionStatus;

import java.util.List;
import java.util.Optional;

public interface CashierSessionRepository {
    CashierSession save(CashierSession session);
    Optional<CashierSession> findById(Long id);
    /** La session actuellement OPEN d'un comptable, s'il y en a une (une seule à la fois, contrainte DB). */
    Optional<CashierSession> findOpenByCashierId(Long cashierId);
    /** Historique des sessions d'un tenant, filtrable par comptable et/ou statut (null = pas de filtre). */
    List<CashierSession> findByTenantId(Long tenantId, Long cashierId, CashierSessionStatus status);
    void deleteById(Long id);
}
