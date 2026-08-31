package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.PlatformFeeLedgerApi;
import gn.lacaeducation.platform.application.port.out.PlatformFeeLedgerRepository;
import gn.lacaeducation.platform.domain.PlatformFeeLedgerEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
class PlatformFeeLedgerApiAdapter implements PlatformFeeLedgerApi {

    private final PlatformFeeLedgerRepository ledgerRepository;

    /** Rejoint la transaction de l'appelant (billing.InvoiceService.applyPayment) — la ligne de
     * registre doit être atomique avec l'application du paiement. */
    @Override
    public void record(Long tenantId, Long invoiceId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        ledgerRepository.save(PlatformFeeLedgerEntry.builder()
                .tenantId(tenantId)
                .invoiceId(invoiceId)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
