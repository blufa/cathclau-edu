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
}
