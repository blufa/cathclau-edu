-- Pointage enseignant : heure d'arrivée (check-in) et retard calculé vs début de son premier cours,
-- pour le récapitulatif mensuel (heures + retard cumulé).
ALTER TABLE teacher_attendances ADD COLUMN IF NOT EXISTS check_in_time TIME;
ALTER TABLE teacher_attendances ADD COLUMN IF NOT EXISTS late_minutes  INTEGER;
