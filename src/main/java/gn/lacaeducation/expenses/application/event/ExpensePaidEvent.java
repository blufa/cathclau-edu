package gn.lacaeducation.expenses.application.event;
import java.math.BigDecimal;
/** Published when an expense is paid — consumed by accounting to record an OUT cash-flow, and by
 * AnnualBudgetService to increment the linked BudgetLine's actualAmount (when budgetLineId is set). */
public record ExpensePaidEvent(Long tenantId, Long expenseId, Long categoryId, Long budgetLineId, BigDecimal amount) {}
