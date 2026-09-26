package gn.lacaeducation.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /** Solde restant dû, pour le mois de paie {@code periodStart}/{@code periodLabel} uniquement, sur
     * les factures de cet élève dont la remise appliquée est un avantage personnel PARTIEL
     * (école-employeur) — le « reste » non pris en charge par l'école, destiné à être retenu
     * automatiquement sur le salaire du parent employé plutôt que facturé au guichet. Jamais la somme
     * de tous les mois déjà générés d'avance (§retour comptable, bug corrigé — une mensualité éclatée
     * en 9 factures dès l'inscription ne doit faire retenir qu'UN mois par fiche de paie). 0 si aucune
     * facture éligible pour ce mois. */
    BigDecimal outstandingStaffBenefitRemainder(Long studentId, LocalDate periodStart, String periodLabel);

    /** Applique {@code amount} comme paiement sur la facture avantage personnel correspondant
     * précisément à ce mois de paie (même portée que {@link #outstandingStaffBenefitRemainder}) —
     * jamais une répartition générique la plus ancienne d'abord (voir {@link #applyMonthlyPayment}),
     * qui risquerait de solder le mauvais mois. */
    void applyStaffBenefitRemainderPayment(Long studentId, LocalDate periodStart, String periodLabel, BigDecimal amount);
}
