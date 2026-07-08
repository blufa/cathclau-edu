package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.Budget;
import java.util.List;
import java.util.Optional;
public interface BudgetRepository {
    Budget save(Budget b);
    Optional<Budget> findById(Long id);
    List<Budget> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
