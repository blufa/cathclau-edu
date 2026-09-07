-- Départ anticipé élève (miroir de teacher_checkins.early_minutes, déjà présent) : sortie pointée
-- avant la fin prévue de la séance. 0 si la sortie n'a jamais été pointée (bénéfice du doute).
ALTER TABLE student_checkins ADD COLUMN IF NOT EXISTS early_minutes INTEGER NOT NULL DEFAULT 0;
