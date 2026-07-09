-- Module 13: annual budget allocations per expense category.
CREATE TABLE IF NOT EXISTS expense_budget (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    category_id      BIGINT NOT NULL REFERENCES expense_category (id),
    academic_year    VARCHAR(20) NOT NULL,
    allocated_amount NUMERIC(15, 2) NOT NULL,
    CONSTRAINT uq_expense_budget UNIQUE (tenant_id, category_id, academic_year)
);
CREATE INDEX IF NOT EXISTS idx_expense_budget_tenant ON expense_budget (tenant_id);
