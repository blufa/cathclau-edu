-- Module Annonces (§8) : annonces de l'établissement, portée toute l'école / classe / élève.
CREATE TABLE announcements (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    content      TEXT,
    scope        VARCHAR(20)  NOT NULL CHECK (scope IN ('GENERAL','CLASS','STUDENT')),
    class_id     BIGINT,
    student_id   BIGINT,
    author_id    BIGINT,
    author_name  VARCHAR(255),
    published_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_announcements_tenant ON announcements (tenant_id);
CREATE INDEX idx_announcements_class ON announcements (class_id);
CREATE INDEX idx_announcements_student ON announcements (student_id);
