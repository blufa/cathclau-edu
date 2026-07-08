-- Workflow d'approbation du budget annuel : motif de rejet (direction) + nouvelle étape SUBMITTED.
ALTER TABLE annual_budgets ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
