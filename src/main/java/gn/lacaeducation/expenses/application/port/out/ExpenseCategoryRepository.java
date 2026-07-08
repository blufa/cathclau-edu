package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.ExpenseCategory;
import java.util.List;
import java.util.Optional;
public interface ExpenseCategoryRepository {
    ExpenseCategory save(ExpenseCategory c);
    Optional<ExpenseCategory> findById(Long id);
    List<ExpenseCategory> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
