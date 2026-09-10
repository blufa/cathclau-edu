package gn.lacaeducation.students.application.event;

/** Published when a validated enrollment is transferred to another class mid-year (target class may
 * have a different fee schedule — e.g. a wrongly-chosen class corrected later). Consumed by academics
 * to recompute both classes' headcount, and by billing to reconcile the student's monthly tuition
 * invoices: the old class's unpaid monthly invoices are voided, any amount already paid on them is
 * carried forward as a credit onto the new class's freshly generated monthly invoices. */
public record EnrollmentClassChangedEvent(Long tenantId, Long studentId, Long enrollmentId,
                                          Long oldClassId, Long newClassId, Long academicYearId) {}
