-- Fusion des deux systèmes de "budget" (annuel vs par catégorie) : une dépense peut désormais
-- s'imputer directement sur une ligne du budget annuel (AnnualBudget/BudgetLine), dont le montant
-- "réalisé" se met alors à jour automatiquement au paiement — l'ancien système d'enveloppe par
-- catégorie (table budget) devient redondant et n'est plus utilisé côté interface.
ALTER TABLE expense ADD COLUMN budget_line_id BIGINT REFERENCES budget_lines(id);
CREATE INDEX idx_expense_budget_line ON expense(budget_line_id);
