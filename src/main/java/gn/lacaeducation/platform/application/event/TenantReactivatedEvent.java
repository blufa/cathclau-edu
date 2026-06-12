package gn.lacaeducation.platform.application.event;

/** Published when a suspended tenant is reactivated. Consumed by iam to restore access. */
public record TenantReactivatedEvent(Long tenantId) {}
