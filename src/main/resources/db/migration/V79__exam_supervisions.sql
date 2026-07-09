-- Surveillance des examens (composition) : affectation d'enseignants surveillants par salle.
CREATE TABLE IF NOT EXISTS exam_supervisions (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    exam_id     BIGINT NOT NULL,
    room_id     BIGINT NOT NULL,
    teacher_id  BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_supervisor UNIQUE (exam_id, teacher_id)
);
CREATE INDEX IF NOT EXISTS idx_exam_supervisions_exam ON exam_supervisions (exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_supervisions_teacher ON exam_supervisions (teacher_id);
