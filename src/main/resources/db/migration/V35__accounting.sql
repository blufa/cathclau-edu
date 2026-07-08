-- Module 14: accounting (SIMPLE level) — settings, fiscal years, treasury accounts and cash-flow.

CREATE TABLE IF NOT EXISTS accounting_settings (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               BIGINT NOT NULL UNIQUE,
    mode                    VARCHAR(16) NOT NULL DEFAULT 'SIMPLE',
    referential             VARCHAR(20),
    currency                VARCHAR(8),
    fiscal_year_start_month INT,
    vat_applicable          BOOLEAN,
    default_vat_rate        NUMERIC(6, 2),
    auto_numbering          BOOLEAN,
    active                  BOOLEAN
);

CREATE TABLE IF NOT EXISTS fiscal_year (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    label            VARCHAR(80) NOT NULL,
    start_date       DATE,
    end_date         DATE,
    status           VARCHAR(12) NOT NULL DEFAULT 'OPEN',
    academic_year_id BIGINT
);
CREATE INDEX IF NOT EXISTS idx_fiscal_year_tenant ON fiscal_year (tenant_id);

CREATE TABLE IF NOT EXISTS treasury_account (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    label           VARCHAR(160) NOT NULL,
    type            VARCHAR(8) NOT NULL,
    bank            VARCHAR(160),
    account_number  VARCHAR(80),
    initial_balance NUMERIC(15, 2),
    current_balance NUMERIC(15, 2),
    currency        VARCHAR(8),
    active          BOOLEAN
);
CREATE INDEX IF NOT EXISTS idx_treasury_account_tenant ON treasury_account (tenant_id);

CREATE TABLE IF NOT EXISTS cash_flow_entry (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    account_id        BIGINT NOT NULL REFERENCES treasury_account (id),
    direction         VARCHAR(4) NOT NULL,
    amount            NUMERIC(15, 2) NOT NULL,
    currency          VARCHAR(8),
    date              DATE,
    category          VARCHAR(16),
    label             VARCHAR(255),
    reference_type    VARCHAR(12),
    reference_id      BIGINT,
    justification_url VARCHAR(512),
    reconciled        BOOLEAN
);
CREATE INDEX IF NOT EXISTS idx_cash_flow_tenant ON cash_flow_entry (tenant_id);
CREATE INDEX IF NOT EXISTS idx_cash_flow_account ON cash_flow_entry (account_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_flow_reference ON cash_flow_entry (reference_type, reference_id)
    WHERE reference_type <> 'MANUAL';
