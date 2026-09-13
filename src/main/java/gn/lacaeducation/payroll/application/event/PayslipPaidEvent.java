package gn.lacaeducation.payroll.application.event;
import java.math.BigDecimal;
/**
 * Published when a payslip is marked paid — consumed by accounting to record an OUT cash-flow
 * (catégorie SALARY) and by communication to notify the teacher/staff member by SMS. Deliberately
 * NOT consumed by the annual (voted) budget module: payroll is always funded directly from the
 * school's revenue (tuition fees), never from the voted operating budget (fuel, kermesse, events) —
 * the two must never be conflated.
 *
 * <p>Carries the name/phone/period directly (frozen on the {@code Payslip} at generation time)
 * rather than just {@code payslipId}, so that {@code communication} can build the SMS without
 * depending on payroll's query API or on the {@code staff}/teacher referential.</p>
 */
public record PayslipPaidEvent(Long tenantId, Long payslipId, BigDecimal amount,
                                String teacherName, String teacherPhone, String periodLabel) {}
