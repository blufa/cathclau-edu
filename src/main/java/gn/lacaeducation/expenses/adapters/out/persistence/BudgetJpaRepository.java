package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, Long> {
    List<BudgetJpaEntity> findByTenantId(Long tenantId);
}
