package gn.lacaeducation.platform.domain;

/**
 * Lifecycle of a school (tenant) — features/01-platform.md §1.
 * <pre>
 *   PENDING_VERIFICATION ──(email verified)──► TRIAL ──(paid)──► ACTIVE
 *                        └─(manual, already paid)─────────────►
 *   TRIAL ──(trial ended, unpaid)──► EXPIRED ──(late subscription)──► ACTIVE
 *   ACTIVE ⇄ SUSPENDED ; ACTIVE|SUSPENDED|EXPIRED ──► ARCHIVED (terminal)
 * </pre>
 * {@code TERMINATED} is kept for backward compatibility; new flows use {@code ARCHIVED}.
 */
public enum TenantStatus {
    PENDING_VERIFICATION, TRIAL, ACTIVE, SUSPENDED, EXPIRED, ARCHIVED, TERMINATED
}
