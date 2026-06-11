package gn.lacaeducation.iam.application.event;

/** Published when a user's password is reset via a reset token. Triggers a security notification. */
public record PasswordResetEvent(Long userId, Long tenantId) {}
