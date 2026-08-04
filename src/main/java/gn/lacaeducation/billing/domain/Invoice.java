package gn.lacaeducation.billing.domain;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}
