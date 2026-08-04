package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Annual budget allocation for an expense category (§5 : gestion budgétaire par soustraction). */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Budget {
    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String academicYear;
    private BigDecimal allocatedAmount;
    /** Cumul des dépenses payées imputées sur cette catégorie — décrémenté automatiquement à chaque
     * paiement de dépense (voir BudgetService#onExpensePaid). */
    private BigDecimal consumedAmount;

    public BigDecimal getRemainingBalance() {
        BigDecimal allocated = allocatedAmount != null ? allocatedAmount : BigDecimal.ZERO;
        BigDecimal consumed = consumedAmount != null ? consumedAmount : BigDecimal.ZERO;
        return allocated.subtract(consumed);
    }
}
