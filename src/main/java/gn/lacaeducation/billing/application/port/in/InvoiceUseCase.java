package gn.lacaeducation.billing.application.port.in;
import gn.lacaeducation.billing.domain.Invoice;
import gn.lacaeducation.billing.domain.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
public interface InvoiceUseCase {
    record CreateInvoiceCommand(Long tenantId, Long studentId, Long feeId, String reference, BigDecimal totalAmount, Long academicYearId) {}
    record BulkGenerateCommand(Long tenantId, Long classId, Long feeId, Long academicYearId) {}
    /** Génère une facture par mois (échéance) du frais MENSUALITE pour un élève, idempotent par échéance. */
    record GenerateMonthlyCommand(Long tenantId, Long studentId, Long feeId, Long academicYearId) {}
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
    List<Invoice> findByTenantIdAndAcademicYearId(Long tenantId, Long academicYearId);
    List<Invoice> findByFeeId(Long feeId);
    boolean existsByStudentIdAndFeeId(Long studentId, Long feeId);
    Invoice applyPayment(Long invoiceId, BigDecimal amount);
    /** Applique une remise / bourse (montant + motif) et recalcule le reste à payer / le statut. */
    Invoice applyDiscount(Long invoiceId, BigDecimal amount, String reason);
    /**
     * Applique la remise pour paiement annuel complet (§billing) sur toutes les factures restantes
     * (non soldées) d'un élève pour un frais donné, si une règle est active pour le type de ce frais.
     * Sans effet (renvoie les factures inchangées) s'il n'y a qu'une seule tranche restante ou si
     * aucune règle n'est applicable.
     */
    List<Invoice> applyEarlyPaymentDiscount(Long studentId, Long feeId);
    /**
     * Annule la remise pour paiement annuel complet précédemment appliquée par
     * {@link #applyEarlyPaymentDiscount} sur les factures restantes d'un élève pour un frais donné —
     * remet chaque facture à son montant initial. À appeler dès que la condition d'éligibilité cesse
     * (ex. le parent décoche un des mois de la scolarité), sans quoi la remise resterait acquise
     * indéfiniment même après un changement de sélection.
     */
    List<Invoice> removeEarlyPaymentDiscount(Long studentId, Long feeId);
    /** Avance d'une étape le recouvrement (relance 1 → relance 2 → exclusion). */
    Invoice advanceRecovery(Long invoiceId);
    /** Positionne directement l'étape de recouvrement. */
    Invoice setRecoveryStage(Long invoiceId, gn.lacaeducation.billing.domain.RecoveryStage stage);
    void cancel(Long id);
    void delete(Long id);
    boolean existsById(Long id);
}
