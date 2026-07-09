-- Responsable légal principal (« responsable 1 ») : un parent désigné comme contact principal de
-- l'élève (cible des factures, relances et notifications), conformément au règlement financier.
ALTER TABLE student_parents ADD COLUMN IF NOT EXISTS primary_guardian BOOLEAN NOT NULL DEFAULT FALSE;
