package gn.lacaeducation.platform;

import java.util.Optional;

/**
 * Provided port (platform public API) giving other modules read-only access to tenant
 * information without depending on platform internals. Owning this here (instead of iam)
 * keeps the dependency direction one-way: iam -> platform (events + tenant status), with no
 * platform -> iam edge, so the modules stay acyclic.
 */
public interface TenantInfoProvider {

    Optional<TenantPublicInfo> findTenantInfo(Long tenantId);

    /**
     * Whether the tenant currently allows its users to access the application.
     * Returns false for suspended/archived/terminated tenants and for unknown ids.
     * A {@code null} tenantId (e.g. SUPER_ADMIN, no tenant scope) is considered active.
     */
    boolean isTenantActive(Long tenantId);
}
