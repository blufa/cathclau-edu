package gn.lacaeducation.billing;
import java.util.Optional;
public interface InvoiceDataProvider {
    Optional<InvoicePublicInfo> findInvoice(Long invoiceId);
    Optional<InvoicePublicInfo> findByReference(String reference);
    /** Libellé d'une échéance ("Octobre 2026", "Tranche 1"…) — pour résoudre une ressource "Installment"
     * du journal d'audit en texte lisible plutôt qu'un id brut. */
    Optional<String> findInstallmentLabel(Long installmentId);
    /** Libellé d'un frais scolaire — même usage que {@link #findInstallmentLabel} pour une ressource
     * "SchoolFee" du journal d'audit. */
    Optional<String> findSchoolFeeLabel(Long feeId);
}
