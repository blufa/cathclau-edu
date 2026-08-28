-- Avantage accordé par la direction (§payroll) : certains enseignants sont payés même pendant les
-- jours fériés/vacances (au lieu de l'exclusion par défaut de ces jours du calcul des heures dues).
ALTER TABLE teacher_compensation ADD COLUMN IF NOT EXISTS paid_during_vacation BOOLEAN NOT NULL DEFAULT FALSE;
