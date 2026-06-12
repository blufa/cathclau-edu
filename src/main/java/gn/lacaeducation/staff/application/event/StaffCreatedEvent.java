package gn.lacaeducation.staff.application.event;

/** Published when a teacher is created (EnseignantCree). Consumed for audit. */
public record StaffCreatedEvent(Long tenantId, Long teacherId, String staffNumber) {}
