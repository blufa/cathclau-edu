package gn.lacaeducation.platform.application.event;

/**
 * Published when a school becomes usable (features/01-platform.md §1): email verified
 * (PENDING_VERIFICATION → TRIAL) or manual activation by SUPER_ADMIN. Consumed by iam (marks the
 * director's account verified/active) and by the platform onboarding listener (creates the current
 * academic year + default settings, then publishes {@link TenantOnboardedEvent}).
 */
public record TenantActivatedEvent(
        Long tenantId,
        String schoolName,
        String adminEmail,
        String director
) {}
