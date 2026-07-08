package gn.lacaeducation.payments;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** API publique du module payments : reçus téléchargeables d'un élève (portail parent §11 — documents). */
public interface StudentReceiptProvider {
    record ReceiptInfo(Long receiptId, String reference, BigDecimal amount, LocalDateTime date) {}

    List<ReceiptInfo> listReceipts(Long studentId);

    /** Élève associé à un reçu (via son paiement), pour le cloisonnement parent lors du téléchargement. */
    java.util.Optional<Long> receiptStudentId(Long receiptId);

    /** Octets PDF d'un reçu (génération à la demande si nécessaire). */
    byte[] receiptPdfBytes(Long receiptId);
}
