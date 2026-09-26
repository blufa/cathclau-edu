package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.MonthlyExpenseSheet;
import java.util.List;
import java.util.Optional;
public interface MonthlyExpenseSheetRepository {
    MonthlyExpenseSheet save(MonthlyExpenseSheet s);
    Optional<MonthlyExpenseSheet> findById(Long id);
    /** Historique complet du tenant, du plus récent au plus ancien (une ligne par mois, §retour comptable). */
    List<MonthlyExpenseSheet> findByTenantId(Long tenantId);
}
