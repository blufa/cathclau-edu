-- Gestion budgétaire par soustraction (§5) : le solde d'une catégorie de dépense est désormais suivi
-- côté serveur, décrémenté automatiquement à chaque paiement de dépense imputée sur cette catégorie.
ALTER TABLE expense_budget ADD COLUMN consumed_amount NUMERIC(15,2) NOT NULL DEFAULT 0;
