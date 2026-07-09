-- Budget annuel complet (Manuel de gestion financière §3) : un budget par année, avec lignes de
-- recettes ET de dépenses, suivi prévu vs réalisé, et approbation par le conseil d'école.
CREATE TABLE IF NOT EXISTS annual_budgets (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    label         VARCHAR(160) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    approved_by   BIGINT,
    approved_at   DATE,
    notes         TEXT,
    created_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_annual_budgets_tenant ON annual_budgets (tenant_id);

CREATE TABLE IF NOT EXISTS budget_lines (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      BIGINT NOT NULL,
    budget_id      BIGINT NOT NULL REFERENCES annual_budgets (id) ON DELETE CASCADE,
    line_type      VARCHAR(8) NOT NULL,
    category       VARCHAR(160) NOT NULL,
    planned_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    actual_amount  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    notes          TEXT
);
CREATE INDEX IF NOT EXISTS idx_budget_lines_budget ON budget_lines (budget_id);
