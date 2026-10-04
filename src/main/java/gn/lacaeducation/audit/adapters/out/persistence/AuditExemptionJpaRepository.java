package gn.lacaeducation.audit.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

interface AuditExemptionJpaRepository extends JpaRepository<AuditExemptionJpaEntity, Long> {

    @Query("SELECT e.validatedBy AS cashierId, COUNT(e) AS exemptionCount "
            + "FROM AuditExemptionJpaEntity e "
            + "WHERE e.tenantId = :tenantId AND e.validatedBy IS NOT NULL AND e.createdAt >= :since "
            + "GROUP BY e.validatedBy")
    List<ExemptionVolumeProjection> aggregateVolumes(
            @Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);

    interface ExemptionVolumeProjection {
        Long getCashierId();
        long getExemptionCount();
    }
}
