-- Snapshot du nom de l'enseignant sur la fiche de paie, pour l'afficher sur le PDF sans coupler
-- le module payroll au module staff (le nom est fourni à la génération).
ALTER TABLE payslips ADD COLUMN IF NOT EXISTS teacher_name VARCHAR(255);
