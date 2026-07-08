package gn.lacaeducation.expenses.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Une ligne d'un budget annuel : prévu vs réalisé, par catégorie. */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BudgetLine {
    private Long id;
    private Long tenantId;
    private Long budgetId;
    private BudgetLineType lineType;
    private String category;
    private BigDecimal plannedAmount;
    private BigDecimal actualAmount;
    private String notes;
}
