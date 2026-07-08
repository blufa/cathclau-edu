-- Module Documents & Modèles : moteur de templates personnalisables par établissement (branding,
-- format, marges, en-têtes/pieds, filigrane, variables dynamiques, numérotation) + historique.
CREATE TABLE IF NOT EXISTS document_templates (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    category        VARCHAR(24) NOT NULL,
    document_type   VARCHAR(40) NOT NULL,
    name            VARCHAR(160) NOT NULL,
    style           VARCHAR(20) NOT NULL DEFAULT 'MODERNE',
    header_html     TEXT,
    body_html       TEXT,
    footer_html     TEXT,
    primary_color   VARCHAR(16),
    font_family     VARCHAR(80),
    logo_url        VARCHAR(1024),
    signature_url   VARCHAR(1024),
    stamp_url       VARCHAR(1024),
    watermark_text  VARCHAR(120),
    page_format     VARCHAR(8) NOT NULL DEFAULT 'A4',
    margin_top      INTEGER NOT NULL DEFAULT 18,
    margin_right    INTEGER NOT NULL DEFAULT 16,
    margin_bottom   INTEGER NOT NULL DEFAULT 18,
    margin_left     INTEGER NOT NULL DEFAULT 16,
    numbering_prefix VARCHAR(40),
    version         INTEGER NOT NULL DEFAULT 1,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_document_templates_tenant ON document_templates (tenant_id);

CREATE TABLE IF NOT EXISTS generated_documents (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL,
    template_id    BIGINT,
    document_type  VARCHAR(40) NOT NULL,
    reference      VARCHAR(60) NOT NULL,
    title          VARCHAR(255),
    subject_type   VARCHAR(24),
    subject_id     BIGINT,
    generated_by   BIGINT,
    generated_at   TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_generated_documents_tenant ON generated_documents (tenant_id);
