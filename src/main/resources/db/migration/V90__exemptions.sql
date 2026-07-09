-- Exonérations / remises exceptionnelles (§8) : traçables et archivées (jamais supprimées).
CREATE TABLE exemptions (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL,
    student_id   BIGINT       NOT NULL,
    type         VARCHAR(20)  NOT NULL CHECK (type IN ('TOTAL','PARTIAL','EXCEPTIONAL')),
    percentage   NUMERIC(5,2),
    amount       NUMERIC(12,2),
    fee_type     VARCHAR(20),
    reason       TEXT         NOT NULL,
    start_date   DATE,
    end_date     DATE,
    validated_by BIGINT,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_exemptions_tenant ON exemptions (tenant_id);
CREATE INDEX idx_exemptions_student ON exemptions (student_id);
