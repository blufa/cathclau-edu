-- Marque une matière affectée à une classe comme « matière de spécialité » (§academics) — utile au
-- lycée, où certaines matières sont la spécialité d'une série précise (ex. Mathématiques en série
-- Scientifique) plutôt qu'une matière générale du tronc commun.

ALTER TABLE class_subjects ADD COLUMN is_specialty BOOLEAN NOT NULL DEFAULT FALSE;
