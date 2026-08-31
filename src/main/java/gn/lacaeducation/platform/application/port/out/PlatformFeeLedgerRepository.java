package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.PlatformFeeLedgerEntry;

import java.math.BigDecimal;
import java.util.List;

public interface PlatformFeeLedgerRepository {
    PlatformFeeLedgerEntry save(PlatformFeeLedgerEntry entry);
    List<PlatformFeeLedgerEntry> findByTenantId(Long tenantId);
    /** Somme des lignes du tenant courant (résolu via le filtre Hibernate tenantFilter). */
    BigDecimal sumForCurrentTenant();
    /** Somme explicite pour un tenant donné (SUPER_ADMIN, hors filtre — contexte tenant nul). */
    BigDecimal sumByTenantId(Long tenantId);
}
