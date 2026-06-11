package gn.lacaeducation.students.application.event;

public record EnrollmentValidatedEvent(Long tenantId, Long studentId, Long classId, Long academicYearId) {}
