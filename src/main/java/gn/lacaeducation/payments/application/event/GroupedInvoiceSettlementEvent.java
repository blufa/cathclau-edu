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
                                            List<Share> shares,
                                            /** Utilisateur ayant physiquement encaissé (canal CASH
                                             * uniquement, cf. {@code Payment.collectedByUserId}) —
                                             * répercuté sur chaque facture-mois soldée. */
                                            Long collectedByUserId) {
    /** Part d'un paiement groupé affectée à une facture-mois, avec l'élève propriétaire (fratrie).
     * {@code paidMonths} : mois "YYYY-MM" couverts par cette part sur une facture multi-mois (tranche
     * partiellement réglée) — null/vide si non précisé. */
    public record Share(Long invoiceId, Long studentId, BigDecimal amount, List<String> paidMonths) {}
}
