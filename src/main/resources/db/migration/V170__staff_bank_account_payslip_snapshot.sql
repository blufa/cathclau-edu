-- Numéro de compte bancaire (RIB/IBAN) optionnel, pour le fichier bancaire de paie.
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS bank_account VARCHAR(64);
ALTER TABLE employees ADD COLUMN IF NOT EXISTS bank_account VARCHAR(64);

-- Téléphone (Mobile Money, priorité) et compte bancaire figés sur la fiche de paie au moment de sa
-- génération (même logique que teacher_name) : le fichier bancaire des périodes déjà générées reste
-- stable même si l'employé change ensuite de numéro/compte.
ALTER TABLE payslips ADD COLUMN IF NOT EXISTS teacher_phone VARCHAR(32);
ALTER TABLE payslips ADD COLUMN IF NOT EXISTS bank_account VARCHAR(64);
