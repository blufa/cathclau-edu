-- Détecteur B3 (§retour fondateurs, contrôle anti-fraude comptable) : écart statistique d'un
-- comptable par rapport à ses pairs du même établissement (écarts de caisse, volume
-- d'exonérations). Une ligne par (tenant, comptable, type), réévaluée et mise à jour en place
-- chaque semaine — jamais dupliquée, voir AccountantAnomalyAlertService#rule.
CREATE TABLE accountant_anomaly_alerts (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT         NOT NULL,
    cashier_id    BIGINT         NOT NULL,
    type          VARCHAR(30)    NOT NULL,
    metric_value  NUMERIC(15,2)  NOT NULL,
    peer_average  NUMERIC(15,2)  NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    resolved_at   TIMESTAMP,
    CONSTRAINT uq_accountant_anomaly_alerts_tenant_cashier_type UNIQUE (tenant_id, cashier_id, type)
);
CREATE INDEX idx_accountant_anomaly_alerts_tenant ON accountant_anomaly_alerts (tenant_id, status);
