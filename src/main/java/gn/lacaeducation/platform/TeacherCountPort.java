package gn.lacaeducation.platform;

/**
 * Provided port (part of the platform module's public API) allowing the platform to read the
 * number of teachers for a tenant, without depending on the staff module. The staff module supplies
 * the implementation — mirrors {@link StudentCountPort} exactly (static dependency staff -> platform,
 * no module cycle). Consumed by the DPE dashboard (cross-tenant headcount aggregation).
 */
public interface TeacherCountPort {
    long countByTenant(Long tenantId);
    long countActiveByTenant(Long tenantId);
}
