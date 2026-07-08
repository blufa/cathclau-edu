-- Subventions scolaires (Manuel §4.3) : recettes hors frais parents (État, ONG, donateurs), avec
-- suivi des versements reçus. Référence les comptes de trésorerie par identifiant uniquement.
CREATE TABLE IF NOT EXISTS grants (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    source          VARCHAR(200) NOT NULL,
    reference       VARCHAR(80),
    purpose         TEXT,
    academic_year   VARCHAR(20),
    total_amount    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    received_amount NUMERIC(15, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'EXPECTED',
    notes           TEXT,
    created_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_grants_tenant ON grants (tenant_id);

CREATE TABLE IF NOT EXISTS grant_payments (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT NOT NULL,
    grant_id      BIGINT NOT NULL REFERENCES grants (id) ON DELETE CASCADE,
    payment_date  DATE NOT NULL,
    amount        NUMERIC(15, 2) NOT NULL,
    method        VARCHAR(40),
    reference     VARCHAR(80),
    treasury_account_id BIGINT,
    notes         TEXT,
    created_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_grant_payments_grant ON grant_payments (grant_id);
