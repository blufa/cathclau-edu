CREATE TABLE webhook_endpoints (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    events TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    endpoint_id BIGINT NOT NULL REFERENCES webhook_endpoints(id),
    event_type VARCHAR(40) NOT NULL CHECK (event_type IN ('PAYMENT_RECEIVED','REPORT_CARD_AVAILABLE','ABSENCE','ENROLLMENT')),
    payload TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    response_code INTEGER,
    next_attempt_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_webhooks (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('ORANGE_MONEY','MTN_MONEY','WAVE','STRIPE')),
    event VARCHAR(100),
    raw_payload TEXT,
    signature VARCHAR(500),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_events_status_next ON webhook_events(status, next_attempt_at);
CREATE INDEX idx_webhook_events_endpoint ON webhook_events(endpoint_id);
CREATE INDEX idx_payment_webhooks_processed ON payment_webhooks(processed);
