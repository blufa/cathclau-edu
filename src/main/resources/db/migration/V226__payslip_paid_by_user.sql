-- Fiche de paie (§payroll) : identité du comptable ayant marqué la fiche payée, pour l'imprimer sous
-- le bloc signature "Signature et Cachet de l'Établissement" (demande utilisateur, nouvelle maquette
-- /features/document-school/Fiche de paie.dc.html).
ALTER TABLE payslips ADD COLUMN IF NOT EXISTS paid_by_user_id BIGINT;
