package gn.lacaeducation.payments.application.port.in;

import gn.lacaeducation.payments.domain.CashDeposit;
import gn.lacaeducation.payments.domain.CashDepositStatus;
import gn.lacaeducation.payments.domain.PaymentChannel;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CashDepositUseCase {
    record CreateCashDepositCommand(Long tenantId, Long accountantId, BigDecimal amount, String currency,
                                     PaymentChannel method, String destinationAccount, LocalDateTime depositedAt,
                                     String transactionReference, String comment, String idempotencyKey) {}

    /** Idempotent sur {@code idempotencyKey} (§5.1 : empêcher les doubles soumissions). Publie
     * {@code CashDepositCreatedEvent} (SMS fondateur) uniquement à la première création réelle. */
    CashDeposit create(CreateCashDepositCommand command);
    CashDeposit findById(Long id);
    /** Historique filtrable (§5.4). */
    List<CashDeposit> search(Long tenantId, Long accountantId, CashDepositStatus status,
                              PaymentChannel method, LocalDateTime from, LocalDateTime to);
    /** Réservé au fondateur (vérifié côté contrôleur ET service) — refuse si déjà décidé (§5.3). */
    CashDeposit validate(Long id, Long founderUserId);
    CashDeposit reject(Long id, Long founderUserId, String reason);
}
