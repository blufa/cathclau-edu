package gn.lacaeducation.payments.application.event;

import java.math.BigDecimal;

/** Publié quand le fondateur valide ou rejette un dépôt (§5.3/§5.4) — {@code status} vaut "VALIDATED"
 * ou "REJECTED" (jamais "PENDING"), {@code method} le nom brut de {@code PaymentChannel} (ex. "CASH").
 * Types déjà "aplatis" en String : les événements inter-modules n'exposent jamais le package
 * {@code domain} (voir {@code payments :: events}, ArchUnit). Consumed by communication (SMS comptable)
 * et par audit. */
public record CashDepositDecidedEvent(Long tenantId, Long depositId, Long accountantId, Long decidedBy,
                                       String status, BigDecimal amount, String currency,
                                       String method, String rejectionReason) {}
