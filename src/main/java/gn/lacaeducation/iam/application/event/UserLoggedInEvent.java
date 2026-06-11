package gn.lacaeducation.iam.application.event;

import java.time.LocalDateTime;

/** Published when a user successfully authenticates. Consumed by the audit trail. */
public record UserLoggedInEvent(Long userId, Long tenantId, LocalDateTime at) {}
