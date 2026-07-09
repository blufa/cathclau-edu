package gn.lacaeducation.expenses.adapters.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface BudgetLineJpaRepository extends JpaRepository<BudgetLineJpaEntity, Long> {
    List<BudgetLineJpaEntity> findByBudgetId(Long budgetId);
    void deleteByBudgetId(Long budgetId);
}
