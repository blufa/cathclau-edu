package gn.lacaeducation.payments.application.event;

import java.math.BigDecimal;

/**
 * Published every time a cashier session is closed (CashierSessionService#close) — even when
 * {@code discrepancy} is zero, so the absence of a gap is itself traceable. Consumed by the audit
 * trail (always) and by communication (SMS alert to the founder/SCHOOL_ADMIN) when the discrepancy
 * is non-zero — §retour fondateurs, contrôle anti-fraude comptable.
 */
public record CashierSessionClosedEvent(Long tenantId, Long cashierId, Long sessionId,
                                        BigDecimal systemTotal, BigDecimal physicalCount,
                                        BigDecimal discrepancy) {}
