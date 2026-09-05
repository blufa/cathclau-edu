-- Module Cours (§16 B) : cours vidéo/PDF payants par classe, achetés par les parents/élèves. Chaque
-- cours possède un frais miroir (school_fees, type OTHER) réutilisant entièrement la facturation et
-- le paiement scolaires déjà en place (Djomy) plutôt qu'un circuit de paiement dédié.
CREATE TABLE courses (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT         NOT NULL,
    class_id       BIGINT         NOT NULL,
    subject_label  VARCHAR(255),
    title          VARCHAR(255)   NOT NULL,
    description    TEXT,
    price          NUMERIC(12,2)  NOT NULL,
    thumbnail_url  VARCHAR(1000),
    school_fee_id  BIGINT         NOT NULL,
    published      BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_courses_tenant ON courses (tenant_id);
CREATE INDEX idx_courses_class ON courses (class_id);

CREATE TABLE course_resources (
    id               BIGSERIAL PRIMARY KEY,
    course_id        BIGINT        NOT NULL,
    type             VARCHAR(10)   NOT NULL CHECK (type IN ('VIDEO','PDF')),
    title            VARCHAR(255),
    file_url         VARCHAR(1000) NOT NULL,
    sort_order       INTEGER,
    duration_seconds INTEGER
);
CREATE INDEX idx_course_resources_course ON course_resources (course_id);

CREATE TABLE course_purchases (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    student_id    BIGINT      NOT NULL,
    course_id     BIGINT      NOT NULL,
    invoice_id    BIGINT      NOT NULL,
    status        VARCHAR(10) NOT NULL CHECK (status IN ('PENDING','PAID')),
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    purchased_at  TIMESTAMP,
    UNIQUE (student_id, course_id)
);
CREATE INDEX idx_course_purchases_tenant ON course_purchases (tenant_id);
CREATE INDEX idx_course_purchases_student ON course_purchases (student_id);
CREATE INDEX idx_course_purchases_invoice ON course_purchases (invoice_id);

INSERT INTO permissions (name, code) VALUES
    ('Manage paid video/PDF courses', 'cours:manage')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SCHOOL_ADMIN','DIRECTOR','RESPONSABLE_PEDAGOGIQUE','TEACHER') AND p.code = 'cours:manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;
