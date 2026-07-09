-- Types d'évaluation configurables par établissement (§3) : l'école crée ses propres types.
CREATE TABLE assessment_type_configs (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    code                VARCHAR(30)  NOT NULL,
    label               VARCHAR(255) NOT NULL,
    default_coefficient NUMERIC(5,2),
    weight              NUMERIC(5,2),
    color               VARCHAR(20),
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_assessment_type_tenant_code UNIQUE (tenant_id, code)
);
CREATE INDEX idx_assessment_type_configs_tenant ON assessment_type_configs (tenant_id);
