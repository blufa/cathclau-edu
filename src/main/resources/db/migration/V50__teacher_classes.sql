-- Affectation directe enseignant ↔ classes (un enseignant peut enseigner plusieurs classes),
-- saisie sur la fiche enseignant sous forme de checklist. Distincte des affectations par matière.
CREATE TABLE teacher_classes (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    class_id   BIGINT NOT NULL,
    UNIQUE (teacher_id, class_id)
);
CREATE INDEX idx_teacher_classes_teacher ON teacher_classes (teacher_id);
