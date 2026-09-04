package gn.lacaeducation.platform.application.port.out;

import gn.lacaeducation.platform.domain.PlatformFeeSettlement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PlatformFeeSettlementRepository {
    PlatformFeeSettlement save(PlatformFeeSettlement settlement);
    Optional<PlatformFeeSettlement> findById(Long id);
    List<PlatformFeeSettlement> findByTenantId(Long tenantId);
    /** Somme des règlements SUCCESSFUL du tenant courant (filtre Hibernate). */
    BigDecimal sumSucceededForCurrentTenant();
    /** Somme explicite pour un tenant donné (SUPER_ADMIN, hors filtre). */
    BigDecimal sumSucceededByTenantId(Long tenantId);
}
