CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    reference VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(15,2) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('ORANGE_MONEY','MTN_MONEY','WAVE','BANK_CARD')),
    type VARCHAR(20) NOT NULL CHECK (type IN ('PARTIAL','COMPLETE')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('INITIATED','PENDING','SUCCEEDED','FAILED','REFUNDED')),
    paid_at TIMESTAMP
);

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id),
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('ORANGE_MONEY','MTN_MONEY','WAVE','STRIPE')),
    external_transaction_id VARCHAR(200),
    provider_status VARCHAR(100),
    response_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL UNIQUE REFERENCES payments(id),
    reference VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(15,2) NOT NULL,
    pdf_file VARCHAR(500),
    generated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_tenant_status ON payments(tenant_id, status);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_student ON payments(student_id);
CREATE INDEX idx_payment_transactions_payment ON payment_transactions(payment_id);
