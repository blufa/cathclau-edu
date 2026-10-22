-- Fenêtre de course réseau anti-double-scan pour le personnel non-enseignant (§16), même mécanisme
-- que pour l'élève/l'enseignant (StudentCheckin/TeacherCheckin, déjà horodatés à la création) :
-- sans cette colonne, impossible de distinguer une retentative réseau du même tap physique d'un
-- vrai second scan humain.
ALTER TABLE employee_attendances ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
