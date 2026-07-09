-- Justification des absences (règlement intérieur) : une absence peut être justifiée ou non,
-- avec un motif (colonne reason existante) et une pièce justificative. Sert aussi à l'alerte
-- « ≥ 2 absences non justifiées dans le mois ».
ALTER TABLE student_attendances ADD COLUMN IF NOT EXISTS justified BOOLEAN;
ALTER TABLE student_attendances ADD COLUMN IF NOT EXISTS justification_doc VARCHAR(1024);
