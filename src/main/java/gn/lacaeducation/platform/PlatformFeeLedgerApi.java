package gn.lacaeducation.platform;

import java.math.BigDecimal;

/**
 * Published API (consumed by the {@code billing} module) : enregistre la part de frais plateforme
 * effectivement encaissée à un paiement de mensualité. Appel synchrone plutôt qu'un événement pour
 * éviter un cycle de dépendance module (billing a déjà besoin de {@link PlatformFeeQuoteApi} dans
 * l'autre sens — platform ne doit jamais dépendre en retour de billing).
 */
public interface PlatformFeeLedgerApi {
    void record(Long tenantId, Long invoiceId, BigDecimal amount);
}
