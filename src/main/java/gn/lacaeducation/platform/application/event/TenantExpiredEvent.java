package gn.lacaeducation.platform.application.event;

/** Published when a school's free trial ends without payment (TRIAL → EXPIRED, §1). */
public record TenantExpiredEvent(Long tenantId) {}
