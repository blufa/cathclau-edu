package gn.lacaeducation.platform.application.event;

/**
 * Published once automatic onboarding of a school completes (features/01-platform.md §2). Consumed by
 * billing-subscription (creates the TRIAL Subscription) and communication (welcome email + director
 * guide). Idempotent: emitted at most once per tenant (guarded by {@code Tenant.onboardedAt}).
 */
public record TenantOnboardedEvent(
        Long tenantId,
        String schoolName,
        String adminEmail
) {}
