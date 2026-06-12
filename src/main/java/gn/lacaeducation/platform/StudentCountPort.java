package gn.lacaeducation.platform;

/**
 * Provided port (part of the platform module's public API) allowing the platform
 * to read the number of students provisioned for a tenant — used to display quota
 * consumption — without depending on the students module. The students module
 * supplies the implementation, so the only static dependency is students -> platform,
 * mirroring {@link TenantQuotaService} and avoiding a module cycle.
 */
public interface StudentCountPort {
    long countByTenant(Long tenantId);
}
