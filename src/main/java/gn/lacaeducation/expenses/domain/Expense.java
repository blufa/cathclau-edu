package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une ligne de dépense au sein d'une {@link MonthlyExpenseSheet} : un article, son montant, et si sa
 * dépense était nécessaire — plus de circuit d'approbation séparé (§retour comptable, point 31) :
 * chaque ligne saisie correspond à un montant déjà dépensé, verrouillée dès que la fiche mensuelle
 * parente est clôturée.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {
    private Long id;
    private Long tenantId;
    /** Fiche de dépense mensuelle à laquelle cette ligne appartient. */
    private Long sheetId;
    private String label;
    private BigDecimal amount;
    /** Coché si la dépense était nécessaire (vs. confort/optionnel) — filtre de lecture pour la direction. */
    private boolean necessary;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
}
