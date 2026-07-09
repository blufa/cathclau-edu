-- Module Cahier de textes / Lesson planning : séances par classe & matière (planifiées puis réalisées),
-- contenu du cours et devoirs donnés. Tenant-scopé ; réutilise classes/matières/enseignants par id.

CREATE TABLE lesson_entries (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    class_id         BIGINT       NOT NULL,
    subject_id       BIGINT       NOT NULL,
    teacher_id       BIGINT,
    lesson_date      DATE         NOT NULL,
    title            VARCHAR(255) NOT NULL,
    content          TEXT,
    homework         TEXT,
    duration_minutes INTEGER,
    status           VARCHAR(16)  NOT NULL DEFAULT 'PLANNED',
    created_at       TIMESTAMP
);
CREATE INDEX idx_lessons_tenant ON lesson_entries (tenant_id);
CREATE INDEX idx_lessons_class ON lesson_entries (class_id);
