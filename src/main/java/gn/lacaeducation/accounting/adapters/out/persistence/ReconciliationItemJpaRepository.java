package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ReconciliationItemJpaRepository extends JpaRepository<ReconciliationItemJpaEntity, Long> {
    List<ReconciliationItemJpaEntity> findByReconciliationId(Long reconciliationId);
    void deleteByReconciliationId(Long reconciliationId);
}
