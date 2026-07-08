-- Module 13: expenses MVP — categories, suppliers and expenses with approval workflow.

CREATE TABLE IF NOT EXISTS expense_category (
    id        BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name      VARCHAR(160) NOT NULL,
    parent_id BIGINT REFERENCES expense_category (id)
);
CREATE INDEX IF NOT EXISTS idx_expense_category_tenant ON expense_category (tenant_id);

CREATE TABLE IF NOT EXISTS supplier (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    name         VARCHAR(200) NOT NULL,
    type         VARCHAR(80),
    phone        VARCHAR(40),
    email        VARCHAR(160),
    address      VARCHAR(255),
    bank_account VARCHAR(80)
);
CREATE INDEX IF NOT EXISTS idx_supplier_tenant ON supplier (tenant_id);

CREATE TABLE IF NOT EXISTS expense (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    category_id      BIGINT REFERENCES expense_category (id),
    supplier_id      BIGINT REFERENCES supplier (id),
    label            VARCHAR(255) NOT NULL,
    amount           NUMERIC(15, 2) NOT NULL,
    currency         VARCHAR(8) NOT NULL DEFAULT 'GNF',
    expense_date     DATE,
    payment_mode     VARCHAR(16),
    reference        VARCHAR(120),
    justification_url VARCHAR(512),
    status           VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(512),
    decided_by       BIGINT,
    created_at       TIMESTAMP,
    submitted_at     TIMESTAMP,
    approved_at      TIMESTAMP,
    paid_at          TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_expense_tenant ON expense (tenant_id);
CREATE INDEX IF NOT EXISTS idx_expense_status ON expense (tenant_id, status);
