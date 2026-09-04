package gn.lacaeducation.common;

import java.util.Set;

/**
 * Holds the set of tenant ids AND prefecture/commune ids visible to a DPE/DCE-scoped request, for
 * the duration of the request. Mirrors {@link TenantContext} but for a set rather than a single
 * value — used exclusively by the DPE dashboard/alerts endpoints, never by ordinary tenant-scoped
 * modules (which keep relying on {@link TenantContext} + the Hibernate {@code tenantFilter}).
 *
 * <p>Deliberately NOT wired into the automatic Hibernate {@code @Filter} mechanism: DPE queries must
 * explicitly filter on this scope ({@code WHERE tenant_id IN (...)}) rather than relying on an
 * implicit, easily-overlooked filter — see DPE_ARCHITECTURE_ANALYSIS.md §7.</p>
 */
public final class DpeScopeContext {

    private static final ThreadLocal<Set<Long>> TENANT_IDS = new ThreadLocal<>();
    private static final ThreadLocal<Set<Long>> PREFECTURE_IDS = new ThreadLocal<>();

    public static void set(Set<Long> tenantIds, Set<Long> prefectureIds) {
        TENANT_IDS.set(tenantIds);
        PREFECTURE_IDS.set(prefectureIds);
    }

    /** Tenant ids visible to the current DPE/DCE request, or {@code null} outside such a request. */
    public static Set<Long> get() { return TENANT_IDS.get(); }

    /** Prefecture/commune ids assigned to the current DPE/DCE account, or {@code null} outside such
     * a request. */
    public static Set<Long> getPrefectureIds() { return PREFECTURE_IDS.get(); }

    public static void clear() {
        TENANT_IDS.remove();
        PREFECTURE_IDS.remove();
    }

    private DpeScopeContext() {}
}
