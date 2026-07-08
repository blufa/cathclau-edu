package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.AnnualBudget;
import java.util.List;
import java.util.Optional;
public interface AnnualBudgetRepository {
    AnnualBudget save(AnnualBudget b);
    Optional<AnnualBudget> findById(Long id);
    List<AnnualBudget> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
