package gn.lacaeducation.billing.application.event;

/**
 * Published when a student is excluded from (or reintegrated into) the staff salary deduction via a
 * billing screen (student invoice page, multi-child collection modal — see
 * InvoiceService#setStaffChildDeductionExcluded). The payroll module listens to this to refresh the
 * "Reste avantage personnel" line of any still-modifiable payslip belonging to this student's staff
 * parent(s) — without it, that amount stays stale when the toggle is done from billing rather than from
 * the payslip detail modal itself (which already triggers the refresh directly).
 */
public record StaffChildDeductionExcludedEvent(Long tenantId, Long studentId, boolean excluded) {}
