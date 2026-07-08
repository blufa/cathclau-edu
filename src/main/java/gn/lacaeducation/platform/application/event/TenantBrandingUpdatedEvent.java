package gn.lacaeducation.platform.application.event;

/** Published when a school's branding (logo, colour) changes — §1, used to refresh public caches. */
public record TenantBrandingUpdatedEvent(Long tenantId) {}
