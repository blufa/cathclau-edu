package gn.lacaeducation.payments.application.event;

import java.math.BigDecimal;

/**
 * Published when a CASH payment is collected by a user who has no open {@code CashierSession} at
 * that moment (§retour fondateurs, contrôle anti-fraude comptable) — encaisser sans jamais passer
 * par le rapprochement de caisse est en soi suspect. Consumed by the audit trail (always) and by
 * communication (SMS alert to the founder/SCHOOL_ADMIN).
 */
public record CashCollectedWithoutOpenSessionEvent(Long tenantId, Long cashierId, Long paymentId,
                                                    BigDecimal amount) {}
