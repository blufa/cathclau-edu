-- Rapprochement de caisse (§angles morts comptables) : ouverture/clôture d'une session de guichet par
-- comptable, avec comparaison du total système (paiements CASH encaissés pendant la session) au
-- comptant physique compté à la clôture.
CREATE TABLE cashier_sessions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    cashier_id BIGINT NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    system_total NUMERIC(15,2),
    physical_count NUMERIC(15,2),
    discrepancy NUMERIC(15,2),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Une seule session ouverte à la fois par comptable.
CREATE UNIQUE INDEX uq_cashier_sessions_open ON cashier_sessions (tenant_id, cashier_id) WHERE status = 'OPEN';
CREATE INDEX idx_cashier_sessions_tenant ON cashier_sessions (tenant_id);
