package gn.lacaeducation.platform.application.event;

/** Published when a subscription is suspended. Consumed by iam to block tenant access. */
public record SubscriptionSuspendedEvent(Long tenantId, Long subscriptionId) {}
