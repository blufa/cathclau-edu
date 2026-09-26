package gn.lacaeducation.billing.application.event;

import java.math.BigDecimal;

/**
 * Published on every payment applied to a school-fee invoice (partial or full). Carries the
 * remaining balance so communication can notify the parent of the amount received and what is
 * still due. A fully settled invoice additionally emits {@link InvoicePaidEvent}.
 *
 * @param collectedByUserId utilisateur ayant physiquement encaissé (canal CASH uniquement, null sinon)
 *                           — permet à {@code communication} d'alerter aussi ce comptable, pas
 *                           uniquement le(s) compte(s) SCHOOL_ADMIN.
 */
public record InvoicePaymentAppliedEvent(Long tenantId, Long studentId, Long invoiceId, String reference,
                                         BigDecimal paidAmount, BigDecimal remainingBalance, Long collectedByUserId) {}
