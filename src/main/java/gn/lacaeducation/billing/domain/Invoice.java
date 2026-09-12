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
}
