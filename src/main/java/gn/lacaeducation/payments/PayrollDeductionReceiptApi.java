package gn.lacaeducation.payments;

import java.math.BigDecimal;

/**
 * API publique du module payments pour le module {@code billing} : enregistre une trace de paiement
 * "système" (retenue sur salaire, §payroll ↔ billing) afin qu'un reçu de paiement existe pour le
 * parent — l'argent n'est jamais physiquement encaissé au guichet ET le solde de la facture a déjà été
 * mis à jour directement par {@code billing} ; cet appel sert uniquement à produire le reçu, jamais à
 * ré-appliquer le paiement sur la facture (voir {@code PaymentUseCase#recordSystemPayment}, qui ne
 * publie aucun événement {@code PaymentSucceeded}).
 */
public interface PayrollDeductionReceiptApi {

    /** @return l'id du paiement créé (pour récupérer le reçu ensuite, {@code GET /payments/{id}/receipt}). */
    Long recordForReceipt(Long tenantId, Long invoiceId, Long studentId, Long academicYearId, BigDecimal amount);
}
