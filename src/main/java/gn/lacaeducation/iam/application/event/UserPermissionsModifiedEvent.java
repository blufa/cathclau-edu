package gn.lacaeducation.iam.application.event;

/** Published when a user's direct permission grants (beyond their role) are modified. Triggers
 * authorization cache invalidation + audit — same effect as {@link PermissionsModifiedEvent}. */
public record UserPermissionsModifiedEvent(Long userId) {}
