-- File d'attente de renvoi des SMS échoués (§retour direction — connexion serveur↔fournisseur SMS
-- instable) : un SMS qui échoue (Nimba injoignable, timeout réseau…) est mis en file au lieu d'être
-- perdu, et renvoyé automatiquement en tâche de fond (même principe que webhook_events).
CREATE TABLE sms_retry_queue (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    recipient_id    BIGINT,
    destination     VARCHAR(32) NOT NULL,
    subject         VARCHAR(255),
    content         TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count   INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP,
    last_error      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sms_retry_queue_status_next_attempt ON sms_retry_queue (status, next_attempt_at);
CREATE INDEX idx_sms_retry_queue_tenant_id ON sms_retry_queue (tenant_id);
