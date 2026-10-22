package gn.lacaeducation.payments.application.port.out;

import gn.lacaeducation.payments.domain.CashDeposit;
import gn.lacaeducation.payments.domain.CashDepositStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CashDepositRepository {
    CashDeposit save(CashDeposit deposit);
    Optional<CashDeposit> findById(Long id);
    /** Verrou pessimiste sur la ligne (§5.3 : protéger contre la double validation et les changements
     * de statut concurrents) — à poser avant de vérifier/changer le statut PENDING. */
    Optional<CashDeposit> lockById(Long id);
    Optional<CashDeposit> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);
    /** Historique filtrable (§5.4) — chaque paramètre optionnel (null = pas de filtre). */
    List<CashDeposit> search(Long tenantId, Long accountantId, CashDepositStatus status,
                              gn.lacaeducation.payments.domain.PaymentChannel method,
                              LocalDateTime from, LocalDateTime to);
}
