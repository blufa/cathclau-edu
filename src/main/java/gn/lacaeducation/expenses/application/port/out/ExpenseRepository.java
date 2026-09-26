package gn.lacaeducation.expenses.application.port.out;
import gn.lacaeducation.expenses.domain.Expense;
import java.util.List;
import java.util.Optional;
public interface ExpenseRepository {
    Expense save(Expense e);
    Optional<Expense> findById(Long id);
    List<Expense> findBySheetId(Long sheetId);
    /** Toutes les lignes de dépense du tenant, tous mois confondus — pour les tableaux de bord
     * agrégés (ex. dashboard fondateur), qui n'ont pas de notion de fiche mensuelle. */
    List<Expense> findByTenantId(Long tenantId);
    void deleteById(Long id);
}
