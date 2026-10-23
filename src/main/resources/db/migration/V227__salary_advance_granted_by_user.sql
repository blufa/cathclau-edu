-- Avance sur salaire (§payroll) : identité du comptable ayant accordé l'avance, pour l'imprimer sous
-- le bloc signature du bon d'avance (demande utilisateur, même principe que paid_by_user_id sur payslips).
ALTER TABLE salary_advances ADD COLUMN IF NOT EXISTS granted_by_user_id BIGINT;
