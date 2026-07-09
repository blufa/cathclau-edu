-- Notes éliminatoires par matière (§7) : seuil sous lequel l'élève est ajourné, paramétrable.
CREATE TABLE eliminatory_rules (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    subject_id     BIGINT       NOT NULL,
    threshold      NUMERIC(5,2) NOT NULL,
    decision_label VARCHAR(30)  NOT NULL DEFAULT 'REDOUBLE',
    active         BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_eliminatory_rules_tenant ON eliminatory_rules (tenant_id);
