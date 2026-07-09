-- Généralise la fiche de paie au personnel non-enseignant (§7.1) : ajoute la nature du salarié et
-- rend teacher_id nullable (il devient l'identifiant générique du salarié dans son référentiel).
ALTER TABLE payslips
    ADD COLUMN employee_type VARCHAR(16) NOT NULL DEFAULT 'TEACHER';

ALTER TABLE payslips
    ALTER COLUMN teacher_id DROP NOT NULL;
