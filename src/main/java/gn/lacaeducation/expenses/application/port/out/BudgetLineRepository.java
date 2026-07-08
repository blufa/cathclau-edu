package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.BudgetLine;
import java.util.List;
import java.util.Optional;
public interface BudgetLineRepository {
    BudgetLine save(BudgetLine l);
    Optional<BudgetLine> findById(Long id);
    List<BudgetLine> findByBudgetId(Long budgetId);
    void deleteById(Long id);
    void deleteByBudgetId(Long budgetId);
}
