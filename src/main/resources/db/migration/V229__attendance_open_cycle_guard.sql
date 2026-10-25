-- Cycles entrée/sortie illimités par jour (§retour terrain : un enfant malade qui sort puis revient
-- de l'hôpital doit pouvoir repointer) — l'ancienne contrainte bloquait toute ré-entrée dans le MÊME
-- créneau/jour (élève/enseignant) ou toute ré-entrée tout court (personnel, une seule ligne/jour).
-- Remplacée par un index unique PARTIEL : au plus UN pointage OUVERT (sans sortie) par personne et
-- par jour — la vraie protection anti-course nécessaire (deux entrées quasi simultanées ne peuvent
-- pas toutes les deux créer un pointage ouvert), qui laisse de côté les pointages CLOS (entrée+sortie
-- déjà faites), désormais illimités par jour, même créneau inclus.
--
-- Nettoyage préalable OBLIGATOIRE avant de poser l'index : des données réelles contiennent déjà
-- PLUSIEURS pointages ouverts (sans sortie) le même jour pour la même personne — héritage de l'ancien
-- modèle "par séance/créneau" (avant le passage à la barrière journalière), où entrer à chaque période
-- sans jamais scanner la sortie entre-temps créait un nouvel OUVERT par créneau. Sans ce nettoyage,
-- CREATE UNIQUE INDEX échoue (clé dupliquée, ex. student_id=12, 2026-09-12). On ferme ici tous les
-- pointages ouverts SAUF le plus récent de chaque jour (celui-là reste le cycle réellement en cours) —
-- jamais de suppression de ligne, juste une heure de sortie de secours pour les anciens, invisibles
-- désormais (pas un vrai scan, juste pour satisfaire l'invariant "un seul ouvert par jour").
UPDATE student_checkins sc
SET check_out_time = COALESCE(sc.session_end, TIME '23:59:59')
WHERE sc.check_out_time IS NULL
  AND sc.id NOT IN (
    SELECT DISTINCT ON (student_id, checkin_date) id
    FROM student_checkins
    WHERE check_out_time IS NULL
    ORDER BY student_id, checkin_date, id DESC
  );
ALTER TABLE student_checkins DROP CONSTRAINT IF EXISTS uq_student_checkin;
CREATE UNIQUE INDEX IF NOT EXISTS uq_student_checkin_open ON student_checkins (student_id, checkin_date) WHERE check_out_time IS NULL;

UPDATE teacher_checkins tc
SET check_out_time = COALESCE(tc.session_end, TIME '23:59:59')
WHERE tc.check_out_time IS NULL
  AND tc.id NOT IN (
    SELECT DISTINCT ON (teacher_id, checkin_date) id
    FROM teacher_checkins
    WHERE check_out_time IS NULL
    ORDER BY teacher_id, checkin_date, id DESC
  );
ALTER TABLE teacher_checkins DROP CONSTRAINT IF EXISTS uq_teacher_checkin;
CREATE UNIQUE INDEX IF NOT EXISTS uq_teacher_checkin_open ON teacher_checkins (teacher_id, checkin_date) WHERE check_out_time IS NULL;

-- Personnel non-enseignant : passe d'UNE ligne par (employé, jour) à PLUSIEURS lignes par jour
-- possibles (un cycle entrée/sortie par ligne, même modèle qu'élève/enseignant). Pas de session_end
-- ici (pas de créneau de cours) : sentinelle fixe pour le même nettoyage préalable.
UPDATE employee_attendances ea
SET check_out_time = TIME '23:59:59'
WHERE ea.check_out_time IS NULL
  AND ea.id NOT IN (
    SELECT DISTINCT ON (employee_id, date) id
    FROM employee_attendances
    WHERE check_out_time IS NULL
    ORDER BY employee_id, date, id DESC
  );
ALTER TABLE employee_attendances DROP CONSTRAINT IF EXISTS uq_employee_attendance;
CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_attendance_open ON employee_attendances (employee_id, date) WHERE check_out_time IS NULL;
