package gn.lacaeducation.students.application.event;

/** Published when a validated enrollment is marked as abandoned (student left in the middle of the
 * school year). Consumed by academics to recompute class headcount, and by billing
 * ({@code EnrollmentAbandonedInvoiceListener}) to cancel the student's still-due invoices (UNPAID/
 * PARTIAL, scoped to this academic year) — the family owes nothing further for months the student
 * won't attend. Amounts already collected are never touched, only what remains due. */
public record EnrollmentAbandonedEvent(Long tenantId, Long studentId, Long classId, Long academicYearId) {}
