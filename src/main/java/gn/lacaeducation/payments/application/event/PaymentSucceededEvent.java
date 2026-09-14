package gn.lacaeducation.payments.application.event;
import java.math.BigDecimal;
/** @param collectedByUserId utilisateur ayant physiquement encaissé (canal CASH uniquement, null sinon —
 *  cf. {@code Payment.collectedByUserId}) ; consommé par l'audit log pour tracer qui a enregistré le paiement. */
public record PaymentSucceededEvent(Long tenantId, Long studentId, Long paymentId, Long invoiceId, BigDecimal amount,
                                    Long collectedByUserId) {}
