package gn.lacaeducation.common;

import java.util.Optional;

/**
 * Shared contract giving every module read-only access to tenant (school) information without
 * depending on the platform module's internals. Lives in {@code common} (the shared kernel, like
 * {@link TenantContext}) so it can be consumed by modules that platform itself depends on
 * (e.g. payments), avoiding a dependency cycle. Platform owns the single implementation.
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
