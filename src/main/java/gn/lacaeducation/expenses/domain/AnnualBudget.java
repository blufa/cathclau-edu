package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Budget annuel de l'école (recettes + dépenses prévisionnelles), Manuel §3. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AnnualBudget {
    private Long id;
    private Long tenantId;
    private String academicYear;
    private String label;
    private BudgetStatus status;
    private Long approvedBy;
    private LocalDate approvedAt;
    private String rejectionReason;
    private String notes;
    /** Comment un budget d'événement (kermesse, sortie…) est financé si la trésorerie ne suffit pas. */
    private FinancingMode financingMode;
    /** Montant à collecter ou à emprunter — pertinent seulement si financingMode ≠ NONE. */
    private java.math.BigDecimal financingAmount;
    /** URL du PDF du budget (généré à la demande, mis en cache), pour la soumission à la direction/fondateur. */
    private String pdfFile;
    private LocalDateTime createdAt;
}
