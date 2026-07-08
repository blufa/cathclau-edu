package gn.lacaeducation.expenses.adapters.out.persistence;
import gn.lacaeducation.expenses.domain.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface ExpenseJpaRepository extends JpaRepository<ExpenseJpaEntity, Long> {
    List<ExpenseJpaEntity> findByTenantId(Long tenantId);
    List<ExpenseJpaEntity> findByTenantIdAndStatus(Long tenantId, ExpenseStatus status);
}
