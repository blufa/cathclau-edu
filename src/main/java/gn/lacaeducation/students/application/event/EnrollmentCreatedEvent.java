package gn.lacaeducation.students.application.event;

import gn.lacaeducation.students.EnrollmentType;

/**
 * Published when an enrollment is created (status PENDING). Consumed by billing to auto-create the
 * ENROLLMENT school-fee invoice so the fee can be collected before the enrollment is validated.
 * No tenantId is carried: billing derives it from the matching SchoolFee.
 */
public record EnrollmentCreatedEvent(Long studentId, Long classId, Long academicYearId,
                                     Long enrollmentId, EnrollmentType type) {}
