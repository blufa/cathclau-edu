package gn.lacaeducation.iam.application.event;

/** Published when a role's permissions are modified. Triggers authorization cache invalidation + audit. */
public record PermissionsModifiedEvent(Long roleId) {}
