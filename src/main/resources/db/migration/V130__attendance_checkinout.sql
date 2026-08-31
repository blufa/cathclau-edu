-- Sortie de séance enseignant : heure de sortie réelle vs fin prévue du créneau.
ALTER TABLE teacher_checkins ADD COLUMN IF NOT EXISTS session_end    TIME;
ALTER TABLE teacher_checkins ADD COLUMN IF NOT EXISTS check_out_time TIME;

-- Pointage élève par SÉANCE (miroir de teacher_checkins) : une ligne par (élève, jour, créneau).
CREATE TABLE IF NOT EXISTS student_checkins (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    student_id        BIGINT NOT NULL,
    class_id          BIGINT NOT NULL,
    checkin_date      DATE NOT NULL,
    schedule_slot_id  BIGINT,
    session_start     TIME,
    session_end       TIME,
    check_in_time     TIME NOT NULL,
    check_out_time    TIME,
    late_minutes      INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP,
    CONSTRAINT uq_student_checkin UNIQUE (student_id, checkin_date, schedule_slot_id)
);
CREATE INDEX IF NOT EXISTS idx_student_checkins_student_date ON student_checkins (student_id, checkin_date);
CREATE INDEX IF NOT EXISTS idx_student_checkins_class_date   ON student_checkins (class_id, checkin_date);

-- Résumé quotidien élève, symétrique à teacher_attendances (arrivée + retard cumulé du jour).
ALTER TABLE student_attendances ADD COLUMN IF NOT EXISTS check_in_time TIME;
ALTER TABLE student_attendances ADD COLUMN IF NOT EXISTS late_minutes  INTEGER;
