-- Niveaux (cycles) où un enseignant est habilité à enseigner (affectation directe, liste déroulante
-- sur sa fiche) — même modèle que teacher_subjects (§V51).
CREATE TABLE teacher_levels (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    level_id   BIGINT NOT NULL,
    UNIQUE (teacher_id, level_id)
);
CREATE INDEX idx_teacher_levels_teacher ON teacher_levels (teacher_id);
