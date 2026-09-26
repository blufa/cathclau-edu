package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fiche de dépense mensuelle (§retour comptable, point 31) : remplace le "Budget annuel" prévisionnel
 * par un concept simple orienté saisie réelle — un libellé (ex. "Dépense du mois d'Octobre"), une
 * année scolaire, un mode de financement le cas échéant, et deux tableaux de lignes ({@link Expense}
 * et {@link RevenueLine}). La clôture ({@link #closedAt}) verrouille définitivement la fiche : plus
 * aucune ligne ne peut y être ajoutée, modifiée ou supprimée — comme la clôture d'une période de notes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyExpenseSheet {
    private Long id;
    private Long tenantId;
    private String label;
    private String academicYear;
    /** Comment cette fiche est financée si la trésorerie courante ne suffit pas (surtout budgets d'événements). */
    private FinancingMode financingMode;
    /** Montant à collecter ou à emprunter — pertinent seulement si financingMode ≠ NONE. */
    private BigDecimal financingAmount;
    private LocalDateTime createdAt;
    private Long createdBy;
    /** Null tant que la fiche est ouverte (modifiable) ; renseigné dès la clôture (verrouillée). */
    private LocalDateTime closedAt;

    public boolean isClosed() {
        return closedAt != null;
    }
}
