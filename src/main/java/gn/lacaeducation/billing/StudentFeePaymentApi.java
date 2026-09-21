package gn.lacaeducation.billing;

import java.math.BigDecimal;

/**
 * API publique du module billing : permet à un autre module qui perçoit déjà de l'argent au nom de
 * l'école par un canal différent du guichet (ex. le module {@code payroll}, qui retient les frais de
 * scolarité des enfants du personnel directement sur le salaire, §payroll ↔ billing) de répercuter ce
 * paiement sur les factures de l'élève concerné — pour que la facturation reste le miroir fidèle de ce
 * qui a réellement été perçu, quel que soit le canal.
 */
public interface StudentFeePaymentApi {
    /** Applique {@code amount} sur les factures de mensualité impayées/partielles de l'élève, de la
     * plus ancienne à la plus récente, jusqu'à épuisement — sans effet si {@code amount} est nul/négatif
     * ou si l'élève n'a aucune facture de mensualité éligible. */
    void applyMonthlyPayment(Long tenantId, Long studentId, BigDecimal amount);

    /** Solde total restant dû sur les factures de mensualité de cet élève dont la remise appliquée est
     * un avantage personnel PARTIEL (école-employeur) — le « reste » non pris en charge par l'école,
     * destiné à être retenu automatiquement sur le salaire du parent employé plutôt que facturé au
     * guichet. 0 si aucune facture éligible. */
    BigDecimal outstandingStaffBenefitRemainder(Long studentId);
}
