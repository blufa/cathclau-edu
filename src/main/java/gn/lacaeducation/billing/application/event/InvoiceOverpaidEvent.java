package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/**
 * Publié quand un encaissement dépasse le solde dû d'une facture (§4 cahier des charges, trop-perçu) —
 * {@code excessAmount} est la part du paiement qui n'a pas pu être imputée à CETTE facture (déjà
 * soldée par le reste). Consommé par {@code StudentCreditFromOverpaymentListener} pour poser ce
 * surplus comme crédit disponible de l'élève, applicable à une facture future.
 */
public record InvoiceOverpaidEvent(Long tenantId, Long studentId, Long invoiceId, Long paymentId,
                                   BigDecimal excessAmount) {}
