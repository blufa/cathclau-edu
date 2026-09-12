-- Lien manuel explicite entre une fiche personnel non-enseignant (employees) et une fiche enseignant
-- (teachers) de la MÊME personne (ex. un directeur qui enseigne aussi quelques heures) — §payroll,
-- fiche de paie combinée (salaire fixe + heures d'enseignement). Nul si la personne n'enseigne pas.
ALTER TABLE employees ADD COLUMN linked_teacher_id BIGINT REFERENCES teachers(id);
CREATE INDEX idx_employees_linked_teacher ON employees (linked_teacher_id) WHERE linked_teacher_id IS NOT NULL;
