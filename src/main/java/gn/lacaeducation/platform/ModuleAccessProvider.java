package gn.lacaeducation.platform;

import gn.lacaeducation.platform.domain.ModuleCode;

import java.util.Set;

/**
 * Platform public API (provided port) letting other modules and the gateway check which functional
 * modules a tenant currently has access to (features/SCHOOL_back_complements.md §M). Keeps the
 * dependency direction one-way into platform, like {@link TenantInfoProvider}.
 */
public interface ModuleAccessProvider {

    /** Effective (active, non-expired) modules of a tenant. A null tenant (SUPER_ADMIN) gets all. */
    Set<ModuleCode> getActiveModules(Long tenantId);

    /** Whether a module is currently active for the tenant. Null tenant (SUPER_ADMIN) ⇒ true. */
    boolean isModuleActive(Long tenantId, ModuleCode moduleCode);
}
