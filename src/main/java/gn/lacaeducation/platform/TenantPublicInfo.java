package gn.lacaeducation.platform;

/**
 * Public view of a tenant (school) exposed to other modules (notification, iam…).
 * Owned by the platform module, which is the source of truth for tenants.
 */
public record TenantPublicInfo(Long id, String schoolName, String email, String phone) {}
