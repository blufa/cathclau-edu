package gn.lacaeducation.students.application.event;

/** Published when a student is created (EleveCree). Consumed for audit / quota tracking. */
public record StudentCreatedEvent(Long tenantId, Long studentId, String studentNumber) {}
