package gn.lacaeducation.payments.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Publié à la création d'un dépôt (§5.2) — consumed by communication pour alerter le fondateur par SMS.
 * {@code method} est le nom brut de {@code PaymentChannel} (ex. "CASH") : les événements inter-modules
 * n'exposent jamais un type du package {@code domain} (voir {@code payments :: events}, ArchUnit). */
public record CashDepositCreatedEvent(Long tenantId, Long depositId, Long accountantId, BigDecimal amount,
                                       String currency, String method, LocalDateTime depositedAt,
                                       String transactionReference) {}
