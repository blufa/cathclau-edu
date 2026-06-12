package gn.lacaeducation.common;

/**
 * Holds the current tenant id for the duration of a request/transaction.
 *
 * <p>Lives in the shared {@code common} module (not in iam) because it is a cross-cutting concern
 * used by every module: keeping it here lets iam depend on platform (events, tenant status) without
 * forming a module cycle, while all modules may still read the bound tenant.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();

    public static void set(Long tenantId) { TENANT.set(tenantId); }
    public static Long get()             { return TENANT.get(); }
    public static void clear()           { TENANT.remove(); }

    private TenantContext() {}
}
