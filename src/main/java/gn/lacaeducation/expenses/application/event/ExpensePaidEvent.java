package gn.lacaeducation.expenses.application.event;
import java.math.BigDecimal;
/** Published when an expense is paid — consumed by accounting to record an OUT cash-flow. */
public record ExpensePaidEvent(Long tenantId, Long expenseId, Long categoryId, BigDecimal amount) {}
