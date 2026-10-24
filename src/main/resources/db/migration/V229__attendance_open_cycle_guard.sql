-- Cycles entrée/sortie illimités par jour (§retour terrain : un enfant malade qui sort puis revient
-- de l'hôpital doit pouvoir repointer) — l'ancienne contrainte bloquait toute ré-entrée dans le MÊME
-- créneau/jour (élève/enseignant) ou toute ré-entrée tout court (personnel, une seule ligne/jour).
-- Remplacée par un index unique PARTIEL : au plus UN pointage OUVERT (sans sortie) par personne et
-- par jour — la vraie protection anti-course nécessaire (deux entrées quasi simultanées ne peuvent
-- pas toutes les deux créer un pointage ouvert), qui laisse de côté les pointages CLOS (entrée+sortie
-- déjà faites), désormais illimités par jour, même créneau inclus.
ALTER TABLE student_checkins DROP CONSTRAINT IF EXISTS uq_student_checkin;
CREATE UNIQUE INDEX IF NOT EXISTS uq_student_checkin_open ON student_checkins (student_id, checkin_date) WHERE check_out_time IS NULL;

ALTER TABLE teacher_checkins DROP CONSTRAINT IF EXISTS uq_teacher_checkin;
CREATE UNIQUE INDEX IF NOT EXISTS uq_teacher_checkin_open ON teacher_checkins (teacher_id, checkin_date) WHERE check_out_time IS NULL;

-- Personnel non-enseignant : passe d'UNE ligne par (employé, jour) à PLUSIEURS lignes par jour
-- possibles (un cycle entrée/sortie par ligne, même modèle qu'élève/enseignant).
ALTER TABLE employee_attendances DROP CONSTRAINT IF EXISTS uq_employee_attendance;
CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_attendance_open ON employee_attendances (employee_id, date) WHERE check_out_time IS NULL;
