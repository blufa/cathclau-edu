package gn.lacaeducation.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    /** Une facture "avantage personnel" de ce mois de paie — détail ligne par ligne (§retour comptable :
     * "afficher pour chaque enfant les factures sur lesquelles tu as retenu"). {@code excluded} : cette
     * facture précise a été retirée au cas par cas de la retenue (§retour utilisateur : "pas obligatoire
     * de prendre tous les frais", ex. Maillots/T-shirts/Tenue scolaire) — si {@code true}, son montant
     * N'EST PAS compté dans {@link #outstandingStaffBenefitRemainder}, elle reste juste listée ici
     * (barrée) pour pouvoir être réincluse. */
    record DeductedInvoiceLine(Long invoiceId, String reference, String feeLabel, BigDecimal amount,
                               String periodLabel, boolean excluded) {}

    /** Détail des factures "avantage personnel" de cet élève pour ce mois de paie, EXCLUES comprises
     * (voir {@code excluded} sur {@link DeductedInvoiceLine}) — contrairement à
     * {@link #outstandingStaffBenefitRemainder}, qui ne compte que celles encore actives. */
    List<DeductedInvoiceLine> outstandingStaffBenefitInvoiceLines(Long studentId, LocalDate periodStart, String periodLabel);

    /** Inclut/exclut une facture précise de la retenue automatique sur salaire — voir {@code excluded}
     * sur {@link DeductedInvoiceLine}. */
    void setInvoiceExcludedFromStaffDeduction(Long invoiceId, boolean excluded);

    /** Comme {@link #setInvoiceExcludedFromStaffDeduction}, mais pour TOUTES les factures "avantage
     * personnel" encore dues de CET élève, tous mois confondus — quand l'enfant lui-même est retiré/
     * réintégré de la retenue (§retour comptable : "si on retire l'enfant de la retenue, ces factures
     * doivent être actives, on doit pouvoir les encaisser"). */
    void setAllStaffBenefitInvoicesExcluded(Long studentId, boolean excluded);

    /** Un frais annexe FACULTATIF (ex. cantine, transport — souvent en plusieurs tranches) du catalogue
     * de la classe de l'élève, pas encore facturé du tout pour lui — jamais une retenue déjà en cours
     * (§retour comptable : "il y a des frais annexe qui ne sont pas obligatoires que nous pouvons
     * retirer dans d'autres mois", à titre d'information seulement, aucun montant n'est retenu tant que
     * la facture n'existe pas). */
    record OptionalFeeNotice(String feeLabel, BigDecimal amount) {}

    /** Frais facultatifs de la classe de l'élève n'ayant encore AUCUNE facture (quel que soit son
     * statut) pour lui — informatif : s'ils sont facturés plus tard, ils entreront automatiquement dans
     * la retenue (§billing, toute facture d'un enfant de personnel est taguée avantage personnel). Vide
     * si l'élève n'a pas d'inscription active ou si la classe n'a aucun frais facultatif. */
    List<OptionalFeeNotice> optionalFeesNotYetInvoiced(Long studentId);
}
