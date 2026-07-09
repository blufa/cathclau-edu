-- Petite caisse (Manuel §7.1) : fonds fixe (float), bons de sortie et réapprovisionnements, avec
-- solde courant. Le réapprovisionnement ramène le solde au niveau du fonds fixe.
CREATE TABLE IF NOT EXISTS petty_cash_funds (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    label           VARCHAR(160) NOT NULL,
    float_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    current_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    custodian       VARCHAR(160),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_petty_cash_funds_tenant ON petty_cash_funds (tenant_id);

CREATE TABLE IF NOT EXISTS petty_cash_transactions (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    fund_id           BIGINT NOT NULL REFERENCES petty_cash_funds (id) ON DELETE CASCADE,
    tx_date           DATE NOT NULL,
    tx_type           VARCHAR(16) NOT NULL,
    amount            NUMERIC(15, 2) NOT NULL,
    voucher_number    VARCHAR(60),
    description       TEXT,
    category          VARCHAR(120),
    recipient         VARCHAR(160),
    justification_url VARCHAR(1024),
    recorded_by       BIGINT,
    created_at        TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_petty_cash_tx_fund ON petty_cash_transactions (fund_id);
