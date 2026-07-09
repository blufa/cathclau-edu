-- Règles paramétrables de retenue sur salaire des frais scolaires des enfants du personnel (§7).
CREATE TABLE salary_fee_deduction_rules (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    label            VARCHAR(255) NOT NULL,
    amount_per_child NUMERIC(12,2) NOT NULL,
    max_children     INTEGER,
    cap              NUMERIC(12,2),
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_salary_fee_deduction_tenant ON salary_fee_deduction_rules (tenant_id);
