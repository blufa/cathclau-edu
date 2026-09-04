package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PlatformFeeSettlementJpaRepository extends JpaRepository<PlatformFeeSettlementJpaEntity, Long> {

    List<PlatformFeeSettlementJpaEntity> findByTenantId(Long tenantId);

    @Query("select coalesce(sum(s.amount), 0) from PlatformFeeSettlementJpaEntity s where s.status = :status")
    BigDecimal sumByStatus(@Param("status") PaymentStatus status);

    @Query("select coalesce(sum(s.amount), 0) from PlatformFeeSettlementJpaEntity s "
            + "where s.tenantId = :tenantId and s.status = :status")
    BigDecimal sumByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") PaymentStatus status);
}
