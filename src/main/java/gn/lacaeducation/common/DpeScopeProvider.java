package gn.lacaeducation.common;

import java.util.Set;

/**
 * Shared contract resolving which tenants (schools) a DPE_ADMIN user may supervise, without the
 * {@code iam} module depending on {@code platform}'s internals — same pattern as
 * {@link TenantInfoProvider}. Platform owns the single implementation (prefecture assignment,
 * DPE_ARCHITECTURE_ANALYSIS.md §5-7). Resolved fresh on every request (not cached in the JWT) so a
 * school added to a prefecture becomes visible immediately, without waiting for a token refresh.
 */
public interface DpeScopeProvider {
    /** Tenant ids visible to this user (empty if the user has no prefecture assignment). */
    Set<Long> tenantIdsForUser(Long userId);

    /** Prefecture/commune ids assigned to this user (empty if none). */
    Set<Long> prefectureIdsForUser(Long userId);

    /** Tenant ids attached to a given prefecture/commune — used by SUPER_ADMIN to preview a DPE
     * dashboard for support/testing (never trusted from a DPE_ADMIN caller, whose scope always comes
     * from {@link #tenantIdsForUser(Long)} instead). */
    Set<Long> tenantIdsForPrefecture(Long prefectureId);

    /** Ids of all active (non-archived) prefectures/communes — used by the daily alert-evaluation
     * job to iterate without depending on platform's internal Prefecture repository. */
    Set<Long> activePrefectureIds();
}
