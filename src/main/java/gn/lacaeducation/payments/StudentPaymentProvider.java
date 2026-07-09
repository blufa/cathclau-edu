package gn.lacaeducation.payments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/** API publique du module payments : dernier paiement réussi d'un élève (pour le reçu). */
public interface StudentPaymentProvider {
    record PaymentInfo(Long studentId, Long invoiceId, String reference, BigDecimal amount,
                       String channel, String status, LocalDateTime date) {}

    Optional<PaymentInfo> latestSucceeded(Long studentId);
    Optional<PaymentInfo> findPayment(Long paymentId);
}
