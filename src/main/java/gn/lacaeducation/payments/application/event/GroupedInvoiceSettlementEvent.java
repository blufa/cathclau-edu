package gn.lacaeducation.payments.application.event;

import java.math.BigDecimal;
import java.util.List;

/**
 * Émis par un paiement groupé (plusieurs mois réglés en une fois) pour répartir le montant sur chaque
 * facture-mois. Consommé par billing qui solde chaque facture individuellement. Distinct de
 * {@link PaymentSucceededEvent} (émis une seule fois pour le total) afin que la comptabilité, les
 * notifications et les webhooks ne comptabilisent l'encaissement qu'une fois.
 */
public record GroupedInvoiceSettlementEvent(Long tenantId, Long studentId, Long paymentId,
                                            List<Share> shares) {
    /** Part d'un paiement groupé affectée à une facture-mois. */
    public record Share(Long invoiceId, BigDecimal amount) {}
}
