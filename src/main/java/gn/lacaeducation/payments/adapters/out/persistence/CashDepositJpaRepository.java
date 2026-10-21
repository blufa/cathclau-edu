package gn.lacaeducation.payments.adapters.out.persistence;

import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface CashDepositJpaRepository extends JpaRepository<CashDepositJpaEntity, Long> {
    Optional<CashDepositJpaEntity> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    /** Verrou pessimiste (§5.3) : voir {@code CashDepositService#decide}. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM CashDepositJpaEntity d WHERE d.id = :id")
    Optional<CashDepositJpaEntity> lockById(@Param("id") Long id);

    @Query("SELECT d FROM CashDepositJpaEntity d WHERE "
            + "d.tenantId = :tenantId AND "
            + "(:accountantId IS NULL OR d.accountantId = :accountantId) AND "
            + "(:status IS NULL OR d.status = :status) AND "
            + "(:method IS NULL OR d.method = :method) AND "
            + "(:from IS NULL OR d.depositedAt >= :from) AND "
            + "(:to IS NULL OR d.depositedAt <= :to) "
            + "ORDER BY d.depositedAt DESC")
    List<CashDepositJpaEntity> search(@Param("tenantId") Long tenantId, @Param("accountantId") Long accountantId,
                                       @Param("status") CashDepositStatus status, @Param("method") PaymentChannel method,
                                       @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
