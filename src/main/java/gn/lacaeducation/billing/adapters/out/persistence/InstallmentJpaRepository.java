package gn.lacaeducation.billing.adapters.out.persistence;
import gn.lacaeducation.billing.domain.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
interface InstallmentJpaRepository extends JpaRepository<InstallmentJpaEntity, Long> {
    List<InstallmentJpaEntity> findByFeeId(Long feeId);

    @Query("SELECT i FROM InstallmentJpaEntity i WHERE i.dueDate < :today AND i.status <> :paid")
    List<InstallmentJpaEntity> findOverdue(@Param("today") LocalDate today, @Param("paid") InstallmentStatus paid);

    // installments n'a pas de tenant_id direct : le tenant se lit via school_fees (fee_id).
    @Query("SELECT i FROM InstallmentJpaEntity i JOIN SchoolFeeJpaEntity sf ON i.feeId = sf.id "
         + "WHERE sf.tenantId = :tenantId AND i.dueDate BETWEEN :from AND :to AND i.status = :pending "
         + "ORDER BY i.dueDate ASC")
    List<InstallmentJpaEntity> findUpcomingByTenant(@Param("tenantId") Long tenantId,
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("pending") InstallmentStatus pending);
}
