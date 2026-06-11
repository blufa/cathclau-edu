package gn.lacaeducation.iam.application.event;

/** Published when a role is assigned to a user. Triggers authorization cache invalidation + audit. */
public record RoleAssignedEvent(Long userId, Long roleId) {}
