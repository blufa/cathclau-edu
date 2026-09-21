package gn.lacaeducation.platform;

import java.math.BigDecimal;

/**
 * Published API (consumed par le module {@code billing}) : enregistre le montant effectivement
 * encaissé sur une facture de frais plateforme (FeeType.PLATFORM, configuré librement par chaque
 * école — voir billing.InvoiceService#applyPayment). Appel synchrone plutôt qu'un événement pour
 * éviter un cycle de dépendance module (platform ne doit jamais dépendre en retour de billing).
 */
public interface PlatformFeeLedgerApi {
    void record(Long tenantId, Long invoiceId, BigDecimal amount);
}
