-- Matières enseignées par un enseignant (affectation directe, liste déroulante sur sa fiche).
CREATE TABLE teacher_subjects (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    UNIQUE (teacher_id, subject_id)
);
CREATE INDEX idx_teacher_subjects_teacher ON teacher_subjects (teacher_id);
