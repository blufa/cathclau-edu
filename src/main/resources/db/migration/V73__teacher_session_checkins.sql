-- Pointage enseignant par SÉANCE : à chaque pointage, on rattache l'arrivée au cours concerné
-- (pas seulement le premier du jour) pour calculer le retard de chaque séance. Le retard du jour est
-- la somme des retards des séances pointées.
CREATE TABLE IF NOT EXISTS teacher_checkins (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    teacher_id        BIGINT NOT NULL,
    checkin_date      DATE NOT NULL,
    schedule_slot_id  BIGINT,
    session_start     TIME,
    check_in_time     TIME NOT NULL,
    late_minutes      INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP,
    CONSTRAINT uq_teacher_checkin UNIQUE (teacher_id, checkin_date, schedule_slot_id)
);
CREATE INDEX IF NOT EXISTS idx_teacher_checkins_teacher_date ON teacher_checkins (teacher_id, checkin_date);
