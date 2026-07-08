-- Module Devoirs (Assignment) : devoirs à rendre par classe/matière + rendus des élèves.
-- Tenant-scopé. Réutilise les classes/matières/enseignants/élèves existants par identifiant.

CREATE TABLE assignments (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    class_id       BIGINT       NOT NULL,
    subject_id     BIGINT       NOT NULL,
    teacher_id     BIGINT,
    title          VARCHAR(255) NOT NULL,
    instructions   TEXT,
    attachment_url VARCHAR(1024),
    due_date       DATE,
    max_score      NUMERIC(6, 2),
    status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    created_at     TIMESTAMP
);
CREATE INDEX idx_assignments_tenant ON assignments (tenant_id);
CREATE INDEX idx_assignments_class ON assignments (class_id);

CREATE TABLE assignment_submissions (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    assignment_id BIGINT      NOT NULL REFERENCES assignments (id) ON DELETE CASCADE,
    student_id    BIGINT      NOT NULL,
    submitted_at  TIMESTAMP,
    status        VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
    file_url      VARCHAR(1024),
    grade         NUMERIC(6, 2),
    feedback      TEXT,
    UNIQUE (assignment_id, student_id)
);
CREATE INDEX idx_asub_assignment ON assignment_submissions (assignment_id);
CREATE INDEX idx_asub_tenant ON assignment_submissions (tenant_id);
