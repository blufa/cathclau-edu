-- Garde-fou : recovery_stage NOT NULL sans défaut faisait échouer l'insertion de facture (inscription).
-- Le code initialise désormais RecoveryStage.NONE ; on backfill les NULL existants et on fixe un défaut.
UPDATE invoices SET recovery_stage = 'NONE' WHERE recovery_stage IS NULL;
ALTER TABLE invoices ALTER COLUMN recovery_stage SET DEFAULT 'NONE';
