package gn.lacaeducation.audit.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface AuditCashierSessionJpaRepository extends JpaRepository<AuditCashierSessionJpaEntity, Long> {

    // COALESCE(..., 0) : un comptable sans aucun écart négatif doit apparaître avec missingTotal=0,
    // pas être absent du groupement (sessionCount compte TOUTES ses sessions closes, pas seulement
    // celles en manquant, pour appliquer le seuil d'éligibilité MIN_SESSIONS_FOR_ELIGIBILITY).
    @Query("SELECT c.cashierId AS cashierId, "
            + "COALESCE(SUM(CASE WHEN c.discrepancy < 0 THEN -c.discrepancy ELSE 0 END), 0) AS missingTotal, "
            + "COUNT(c) AS sessionCount "
            + "FROM AuditCashierSessionJpaEntity c "
            + "WHERE c.tenantId = :tenantId AND c.status = 'CLOSED' AND c.closedAt >= :since "
            + "GROUP BY c.cashierId")
    List<CashDiscrepancyProjection> aggregateDiscrepancies(
            @Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);

    interface CashDiscrepancyProjection {
        Long getCashierId();
        java.math.BigDecimal getMissingTotal();
        long getSessionCount();
    }
}
