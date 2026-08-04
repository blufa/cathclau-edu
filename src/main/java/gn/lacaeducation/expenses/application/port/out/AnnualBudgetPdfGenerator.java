package gn.lacaeducation.expenses.application.port.out;

import gn.lacaeducation.expenses.domain.AnnualBudget;
import gn.lacaeducation.expenses.domain.BudgetLine;

import java.util.List;

/** Génère le PDF récapitulatif d'un budget (lignes + total + financement), pour soumission à la
 * direction/au fondateur. */
public interface AnnualBudgetPdfGenerator {
    byte[] generate(AnnualBudget budget, List<BudgetLine> lines);
}
