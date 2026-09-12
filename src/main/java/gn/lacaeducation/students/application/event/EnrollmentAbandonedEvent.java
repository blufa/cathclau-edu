package gn.lacaeducation.students.application.event;

/** Published when a validated enrollment is marked as abandoned (student left in the middle of the
 * school year). Consumed by academics to recompute class headcount — distinct from
 * {@link EnrollmentCancelledEvent}, which the billing/attendance/grading modules already treat
 * differently (a cancellation excludes the student from recovery calculations, an abandonment does
 * not: unpaid invoices from before the departure still count toward the class's recovery rate). */
public record EnrollmentAbandonedEvent(Long tenantId, Long studentId, Long classId, Long academicYearId) {}
