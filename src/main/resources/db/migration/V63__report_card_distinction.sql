-- Appréciation du conseil de classe sur le bulletin (Félicitations, Compliments, Encouragements,
-- Tableau d'honneur, Mise en garde) — calculée automatiquement et ajustable manuellement.
ALTER TABLE report_cards ADD COLUMN IF NOT EXISTS distinction VARCHAR(24);
