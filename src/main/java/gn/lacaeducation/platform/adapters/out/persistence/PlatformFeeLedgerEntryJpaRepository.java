package gn.lacaeducation.platform.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PlatformFeeLedgerEntryJpaRepository extends JpaRepository<PlatformFeeLedgerEntryJpaEntity, Long> {

    List<PlatformFeeLedgerEntryJpaEntity> findByTenantId(Long tenantId);

    /** Filtré automatiquement au tenant courant par tenantFilter ; somme globale si le filtre est
     * désactivé (SUPER_ADMIN). */
    @Query("select coalesce(sum(e.amount), 0) from PlatformFeeLedgerEntryJpaEntity e")
    BigDecimal sumAll();

    @Query("select coalesce(sum(e.amount), 0) from PlatformFeeLedgerEntryJpaEntity e where e.tenantId = :tenantId")
    BigDecimal sumByTenantId(@Param("tenantId") Long tenantId);
}
