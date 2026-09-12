package gn.lacaeducation.billing.application.service;

import gn.lacaeducation.billing.application.port.in.InvoiceUseCase;
import gn.lacaeducation.payments.application.event.GroupedInvoiceSettlementEvent;
import gn.lacaeducation.payments.application.event.PaymentRefundedEvent;
import gn.lacaeducation.payments.application.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates the school-fee invoice when a payment succeeds (PaiementReussi → maj facture, §18).
 * Applies the paid amount, which recomputes the remaining balance and derives the invoice status
 * (UNPAID → PARTIAL → PAID). A fully settled invoice triggers InvoicePaidEvent from the service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePaymentListener {

    private final InvoiceUseCase invoiceUseCase;

    @EventListener
    @Transactional
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event.invoiceId() == null || event.amount() == null) {
            log.debug("PaymentSucceeded without invoice/amount, skipping invoice update (payment id={})", event.paymentId());
            return;
        }
        if (!invoiceUseCase.existsById(event.invoiceId())) {
            log.warn("PaymentSucceeded references unknown invoice id={}, skipping", event.invoiceId());
            return;
        }
        invoiceUseCase.applyPayment(event.invoiceId(), event.amount());
        log.info("Applied payment id={} amount={} to invoice id={}",
                event.paymentId(), event.amount(), event.invoiceId());
    }

    /**
     * PaiementRembourse (§9.1) : répercute le remboursement sur la facture (paidAmount/solde/statut).
     * Symétrique de {@link #onPaymentSucceeded} — même garde (ignore si le paiement n'est pas
     * rattaché à une facture précise, cas d'un paiement groupé, non couvert ici : le remboursement
     * d'un règlement groupé nécessiterait de connaître la répartition par facture, comme pour
     * {@link GroupedInvoiceSettlementEvent}, absente de {@code PaymentRefundedEvent} aujourd'hui).
     */
    @EventListener
    @Transactional
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        if (event.invoiceId() == null || event.amount() == null) {
            log.debug("PaymentRefunded without invoice/amount, skipping invoice update (payment id={})", event.paymentId());
            return;
        }
        if (!invoiceUseCase.existsById(event.invoiceId())) {
            log.warn("PaymentRefunded references unknown invoice id={}, skipping", event.invoiceId());
            return;
        }
        invoiceUseCase.reversePayment(event.invoiceId(), event.amount());
        log.info("Reversed payment id={} amount={} on invoice id={}",
                event.paymentId(), event.amount(), event.invoiceId());
    }

    /**
     * Paiement groupé : solde chaque facture-mois selon la part qui lui revient, SANS notification
     * individuelle par facture ({@code notify=false}) — le parent reçoit déjà une notification unique
     * pour tout le règlement via {@code PaymentSucceededEvent}, qui ne se déclenche (côté
     * communication) que pour les paiements non rattachés à une facture précise, exactement le cas
     * d'un paiement groupé. Sans ce {@code notify=false}, chaque facture couverte (ex. inscription +
     * tranche 1 + tranche 2 réglées en une fois) déclenchait sa propre notification, en plus de celle
     * du règlement global.
     */
    @EventListener
    @Transactional
    public void onGroupedSettlement(GroupedInvoiceSettlementEvent event) {
        for (GroupedInvoiceSettlementEvent.Share share : event.shares()) {
            if (share.invoiceId() == null || share.amount() == null) continue;
            if (!invoiceUseCase.existsById(share.invoiceId())) {
                log.warn("Paiement groupé {} : facture inconnue id={}, ignorée", event.paymentId(), share.invoiceId());
                continue;
            }
            invoiceUseCase.applyPayment(share.invoiceId(), share.amount(), false, share.paidMonths());
            log.info("Paiement groupé {} : {} appliqué à la facture id={}",
                    event.paymentId(), share.amount(), share.invoiceId());
        }
    }
}
