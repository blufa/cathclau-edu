-- Workflow de recouvrement (contexte écoles privées Guinée) : étape (relances → exclusion de l'élève)
-- et date de la dernière relance. Pas de mise en demeure / contentieux.
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS recovery_stage   VARCHAR(16) NOT NULL DEFAULT 'NONE';
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS last_reminder_at DATE;
