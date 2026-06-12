package gn.lacaeducation.students.application.event;

/** Published when an enrollment is cancelled. Consumed by academics to recompute class headcount. */
public record EnrollmentCancelledEvent(Long tenantId, Long studentId, Long classId, Long academicYearId) {}
