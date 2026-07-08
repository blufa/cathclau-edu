package gn.lacaeducation.billing;

import java.math.BigDecimal;
import java.util.Optional;

/** API publique du module billing : dernière facture d'un élève (pour les documents : facture, échéancier). */
public interface StudentBillingProvider {
    record InvoiceInfo(Long studentId, String reference, BigDecimal total, BigDecimal paid, BigDecimal remaining,
                       String status, String periodLabel) {}

    Optional<InvoiceInfo> latestInvoice(Long studentId);
    Optional<InvoiceInfo> findInvoice(Long invoiceId);
}
