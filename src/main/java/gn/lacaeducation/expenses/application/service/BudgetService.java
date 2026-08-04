package gn.lacaeducation.expenses.application.service;

import gn.lacaeducation.common.ResourceNotFoundException;
import gn.lacaeducation.expenses.application.event.ExpensePaidEvent;
import gn.lacaeducation.expenses.application.port.out.BudgetRepository;
import gn.lacaeducation.expenses.domain.Budget;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * CRUD for annual budget allocations per category, avec gestion budgétaire par soustraction (§5) :
 * le solde ({@link Budget#getRemainingBalance()}) est décrémenté automatiquement à chaque paiement
 * d'une dépense imputée sur la catégorie ({@link #onExpensePaid}), sans agrégation côté client.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository repo;

    public record BudgetCommand(Long tenantId, Long categoryId, String academicYear, BigDecimal allocatedAmount) {}

    public Budget save(BudgetCommand c) {
        return repo.save(Budget.builder().tenantId(c.tenantId()).categoryId(c.categoryId())
                .academicYear(c.academicYear()).allocatedAmount(c.allocatedAmount())
                .consumedAmount(BigDecimal.ZERO).build());
    }

    public Budget update(Long id, BigDecimal allocatedAmount) {
        Budget b = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget introuvable : " + id));
        b.setAllocatedAmount(allocatedAmount);
        return repo.save(b);
    }

    @Transactional(readOnly = true)
    public List<Budget> findByTenant(Long tenantId) { return repo.findByTenantId(tenantId); }

    public void delete(Long id) { repo.deleteById(id); }

    /** Soustraction automatique : impute le montant payé sur le(s) budget(s) de cette catégorie. */
    @EventListener
    public void onExpensePaid(ExpensePaidEvent event) {
        repo.findByTenantId(event.tenantId()).stream()
                .filter(b -> event.categoryId().equals(b.getCategoryId()))
                .forEach(b -> {
                    BigDecimal consumed = b.getConsumedAmount() != null ? b.getConsumedAmount() : BigDecimal.ZERO;
                    b.setConsumedAmount(consumed.add(event.amount()));
                    repo.save(b);
                });
    }
}
