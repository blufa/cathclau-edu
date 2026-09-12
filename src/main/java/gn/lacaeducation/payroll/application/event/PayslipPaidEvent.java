package gn.lacaeducation.payroll.application.event;
import java.math.BigDecimal;
/**
 * Published when a payslip is marked paid — consumed by accounting to record an OUT cash-flow
 * (catégorie SALARY). Deliberately NOT consumed by the annual (voted) budget module: payroll is
 * always funded directly from the school's revenue (tuition fees), never from the voted operating
 * budget (fuel, kermesse, events) — the two must never be conflated.
 */
public record PayslipPaidEvent(Long tenantId, Long payslipId, BigDecimal amount) {}
