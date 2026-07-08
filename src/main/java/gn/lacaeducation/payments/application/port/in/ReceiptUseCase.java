package gn.lacaeducation.payments.application.port.in;
import gn.lacaeducation.payments.domain.Receipt;
import java.util.List;
import java.util.Optional;
public interface ReceiptUseCase {
    Receipt generate(Long paymentId);
    /** Génère un reçu groupé (plusieurs mois / année) listant les mois couverts et le reste à payer. */
    Receipt generateGrouped(Long paymentId, String coveredMonths, Integer monthsCount, java.math.BigDecimal remainingAfter);
    /** Renvoie l'URL du PDF du reçu, en le générant/uploadant à la demande s'il manque encore. */
    String pdfUrl(Long receiptId);
    /** Génère et renvoie les octets du PDF du reçu (pour affichage direct, sans Cloudinary). */
    byte[] pdfBytes(Long receiptId);
    Optional<Receipt> findById(Long id);
    Optional<Receipt> findByPaymentId(Long paymentId);
    Optional<Receipt> findByReference(String reference);
    List<Receipt> findByStudentId(Long studentId);
}
