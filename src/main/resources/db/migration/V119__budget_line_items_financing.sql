-- Budget d'événement (kermesse, sortie…) : lignes libres avec quantité × prix unitaire optionnels,
-- mode de financement (collecte / prêt) quand la trésorerie courante ne suffit pas, et PDF du budget
-- (mis en cache) pour la soumission à la direction/au fondateur.
ALTER TABLE budget_lines ADD COLUMN quantity INTEGER;
ALTER TABLE budget_lines ADD COLUMN unit_price NUMERIC(15,2);

ALTER TABLE annual_budgets ADD COLUMN financing_mode VARCHAR(16);
ALTER TABLE annual_budgets ADD COLUMN financing_amount NUMERIC(15,2);
ALTER TABLE annual_budgets ADD COLUMN pdf_file VARCHAR(1024);
