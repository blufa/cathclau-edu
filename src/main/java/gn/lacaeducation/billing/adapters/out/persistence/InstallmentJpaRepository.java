package gn.lacaeducation.billing.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface InstallmentJpaRepository extends JpaRepository<InstallmentJpaEntity, Long> {
    List<InstallmentJpaEntity> findByFeeId(Long feeId);
}
