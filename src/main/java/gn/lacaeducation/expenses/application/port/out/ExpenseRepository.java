package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.Expense;
import gn.lacaeducation.expenses.domain.ExpenseStatus;
import java.util.List;
import java.util.Optional;
public interface ExpenseRepository {
    Expense save(Expense e);
    Optional<Expense> findById(Long id);
    List<Expense> findByTenantId(Long tenantId);
    List<Expense> findByTenantIdAndStatus(Long tenantId, ExpenseStatus status);
    void deleteById(Long id);
}
