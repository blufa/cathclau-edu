package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface InvoiceUseCase {
    record CreateInvoiceCommand(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount) {}
    record BulkGenerateCommand(Long tenantId, Long classId, Long feeId) {}
    /** Génère une facture par mois (échéance) du frais MENSUALITE pour un élève, idempotent par échéance. */
    record GenerateMonthlyCommand(Long tenantId, Long studentId, Long feeId) {}
    Invoice create(CreateInvoiceCommand command);
    List<Invoice> generateMonthly(GenerateMonthlyCommand command);
    List<Invoice> generateBulk(BulkGenerateCommand command);
    /** Renvoie l'URL du PDF de la facture, en le générant/uploadant à la demande s'il manque encore. */
    String pdfUrl(Long invoiceId);
    /** Génère et renvoie les octets du PDF de la facture (pour affichage direct, sans Cloudinary). */
    byte[] pdfBytes(Long invoiceId);
    Optional<Invoice> findById(Long id);
    Optional<Invoice> findByReference(String reference);
    List<Invoice> findByStudentId(Long studentId);
    List<Invoice> findByTenantId(Long tenantId);
    List<Invoice> findByTenantIdAndStatus(Long tenantId, InvoiceStatus status);
    List<Invoice> findByFeeId(Long feeId);
    boolean existsByStudentIdAndFeeId(Long studentId, Long feeId);
    Invoice applyPayment(Long invoiceId, BigDecimal amount);
    /** Applique une remise / bourse (montant + motif) et recalcule le reste à payer / le statut. */
    Invoice applyDiscount(Long invoiceId, BigDecimal amount, String reason);
    /** Avance d'une étape le recouvrement (relance 1 → relance 2 → exclusion). */
    Invoice advanceRecovery(Long invoiceId);
    /** Positionne directement l'étape de recouvrement. */
    Invoice setRecoveryStage(Long invoiceId, gn.lacaeducation.billing.domain.RecoveryStage stage);
    void cancel(Long id);
    void delete(Long id);
    boolean existsById(Long id);
}
