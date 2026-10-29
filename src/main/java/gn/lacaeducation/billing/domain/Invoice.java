package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Invoice {
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long feeId;
    /** Année scolaire à laquelle rattacher la facture — évite qu'une facture d'une année clôturée
     * réapparaisse dans l'année en cours. */
    private Long academicYearId;
    private String reference;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingBalance;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private String pdfFile;
    /** Échéance (mois) couverte par cette facture, pour les frais MENSUALITE ; null sinon. */
    private Long installmentId;
    /** Libellé du mois facturé (ex. « Octobre 2025 »), affiché au parent. */
    private String periodLabel;
    /** Remise / bourse appliquée (montant). Net dû = total - remise. */
    private BigDecimal discountAmount;
    /** Motif de la remise (bourse, remise d'ordre Ramadan, réduction fratrie…). */
    private String discountReason;
    /** Origine de la remise en % effectivement appliquée (la plus avantageuse des trois) — voir
     * {@link DiscountSource}. Null si aucune remise en % (seule une remise EXCEPTIONAL éventuelle,
     * ou aucune remise du tout). */
    private DiscountSource discountSource;
    /** Étape de recouvrement (relances → exclusion de l'élève). */
    private RecoveryStage recoveryStage;
    /** Date de la dernière relance. */
    private LocalDate lastReminderAt;
    /** Part de {@link #totalAmount} due à l'éditeur de la plateforme (frais d'usage, figé au moment de
     * la génération de la facture — un changement de tarif ne modifie jamais les factures déjà émises).
     * Null/zéro si aucun frais plateforme n'était configuré au moment de la génération. */
    private BigDecimal platformFeeAmount;
    /** Sous-ensemble de mois "YYYY-MM" (parmi ceux de l'échéance liée, {@code Installment.months})
     * effectivement réglés jusqu'ici — pour une facture multi-mois payée en plusieurs fois (ex. une
     * tranche de 3 mois réglée en 2 encaissements de 2 puis 1 mois), la facture doit refléter quels
     * mois sont couverts, pas seulement un solde global. Vide/null pour une facture d'un seul mois
     * (le periodLabel suffit alors). Alimenté par InvoiceUseCase#applyPayment. */
    private List<String> paidMonths;
    /** Utilisateur ayant physiquement encaissé le dernier paiement appliqué à cette facture (canal
     * CASH uniquement) — affiché en pied de facture PDF ("Encaissé par"). Null tant qu'aucun paiement
     * guichet n'a encore été appliqué. */
    private Long collectedByUserId;
    /** Montant porté à {@link #paidAmount} SANS paiement réel correspondant (ex. crédit reporté d'une
     * ancienne classe lors d'un transfert, voir ClassTransferInvoiceListener#reconcileMonthly) — sans
     * cette trace, le comptable voit une facture déjà réglée sans aucun encaissement associé. Null/zéro
     * si aucun crédit n'a jamais été reporté sur cette facture. */
    private BigDecimal creditedAmount;
    /** Motif du crédit reporté (ex. "Crédit reporté de l'ancienne classe (transfert)"). Null si
     * {@link #creditedAmount} est null/zéro. */
    private String creditReason;
    /** Date/heure à laquelle le crédit a été reporté — affichée à la place du paiement (aucun
     * encaissement direct n'existe pour un crédit) dans la colonne "Date & heure" du reçu. */
    private java.time.LocalDateTime creditedAt;
    /** Cette facture précise (ex. un frais annexe facultatif comme Maillots/T-shirts/Tenue scolaire)
     * est exclue de la retenue automatique sur salaire — réglable par le comptable depuis le modal
     * "Détail de la retenue" (§payroll), facture par facture, sans toucher aux autres factures de
     * l'enfant ni au réglage global du parent ({@code Parent#excludeFromAutoDeduction}). Le parent
     * règle alors celle-ci lui-même, normalement, au guichet. Jamais null en base (défaut false) —
     * {@code @Builder.Default} obligatoire : Hibernate insère TOUJOURS explicitement la valeur Java de
     * ce champ (même {@code null}), le {@code DEFAULT false} SQL de la colonne ne joue que si la
     * colonne est absente de l'INSERT, jamais quand elle vaut explicitement NULL — sans ce défaut,
     * CHAQUE nouvelle facture (donc toute inscription d'élève) violait la contrainte NOT NULL en base
     * et échouait en 500 (incident réel constaté juste après la migration V233). */
    @Builder.Default
    private Boolean excludedFromStaffDeduction = false;
}
