package gn.lacaeducation.accounting.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface BankReconciliationJpaRepository extends JpaRepository<BankReconciliationJpaEntity, Long> {
    List<BankReconciliationJpaEntity> findByTenantIdOrderByStatementDateDesc(Long tenantId);
}
