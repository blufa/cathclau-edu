-- Rapprochement bancaire (Manuel §8) : comparer le solde du relevé bancaire au solde du livre
-- (compte de trésorerie), en tenant compte des éléments en suspens (chèques non débités, dépôts en
-- transit, frais/intérêts bancaires). Rapprochement réussi quand l'écart ajusté est nul.
CREATE TABLE IF NOT EXISTS bank_reconciliations (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    treasury_account_id BIGINT NOT NULL,
    statement_date      DATE NOT NULL,
    statement_balance   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    book_balance        NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    notes               TEXT,
    reconciled_by       BIGINT,
    created_at          TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bank_recon_tenant ON bank_reconciliations (tenant_id);

CREATE TABLE IF NOT EXISTS reconciliation_items (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    reconciliation_id BIGINT NOT NULL REFERENCES bank_reconciliations (id) ON DELETE CASCADE,
    item_type         VARCHAR(24) NOT NULL,
    description       VARCHAR(255),
    amount            NUMERIC(15, 2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_recon_items_recon ON reconciliation_items (reconciliation_id);
