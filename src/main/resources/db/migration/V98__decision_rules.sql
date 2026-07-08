-- Moteur de règles de décision paramétrable (§17), activable/désactivable et versionné (§8).
CREATE TABLE decision_rules (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    name           VARCHAR(255) NOT NULL,
    condition_type VARCHAR(30)  NOT NULL,
    threshold      NUMERIC(5,2),
    subject_id     BIGINT,
    min_count      INTEGER,
    decision       VARCHAR(30)  NOT NULL,
    priority       INTEGER      NOT NULL DEFAULT 100,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    valid_from     DATE,
    valid_to       DATE
);
CREATE INDEX idx_decision_rules_tenant ON decision_rules (tenant_id);
