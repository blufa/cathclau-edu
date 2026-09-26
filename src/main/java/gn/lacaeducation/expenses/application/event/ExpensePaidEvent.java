package gn.lacaeducation.expenses.application.event;
import java.math.BigDecimal;
/** Published when an expense line is recorded — every line entered is an amount already spent (no
 * approval workflow, §retour comptable point 31) — consumed by accounting to record an OUT cash-flow. */
public record ExpensePaidEvent(Long tenantId, Long expenseId, BigDecimal amount) {}
