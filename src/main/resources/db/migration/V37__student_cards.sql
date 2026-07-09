-- Module 15: student cards with signed QR — templates and issued cards.
CREATE TABLE IF NOT EXISTS card_template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(160) NOT NULL,
    logo_url        VARCHAR(512),
    primary_color   VARCHAR(16),
    secondary_color VARCHAR(16),
    background_url  VARCHAR(512),
    orientation     VARCHAR(12),
    recto_config    TEXT,
    verso_config    TEXT,
    is_default      BOOLEAN,
    active          BOOLEAN
);
CREATE INDEX IF NOT EXISTS idx_card_template_tenant ON card_template (tenant_id);

CREATE TABLE IF NOT EXISTS student_card (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    student_id        BIGINT NOT NULL,
    card_template_id  BIGINT,
    academic_year_id  BIGINT,
    class_id          BIGINT,
    student_number    VARCHAR(60),
    card_number       VARCHAR(60),
    qr_payload        TEXT,
    qr_signature      TEXT,
    photo_url         VARCHAR(512),
    issue_date        DATE,
    expiration_date   DATE,
    status            VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',
    pdf_url           VARCHAR(512),
    replaces_card_id  BIGINT,
    created_at        TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_student_card_tenant ON student_card (tenant_id);
CREATE INDEX IF NOT EXISTS idx_student_card_student ON student_card (student_id);
