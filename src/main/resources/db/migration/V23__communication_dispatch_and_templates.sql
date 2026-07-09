-- Auto-notifications are system-generated (no human sender) and may target contacts
-- without a user account, so relax the NOT NULL constraints and record the dispatch destination.
ALTER TABLE communications ALTER COLUMN sender_id DROP NOT NULL;
ALTER TABLE message_recipients ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE message_recipients ADD COLUMN destination VARCHAR(255);

-- §11.2 (BONUS): per-tenant message templates with {{variable}} placeholders.
CREATE TABLE message_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('LATE','ABSENCE','PAYMENT_DUE','RESULT','MEETING','GENERAL')),
    channel VARCHAR(20) CHECK (channel IN ('SMS','EMAIL','WHATSAPP')),
    subject VARCHAR(255),
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_message_templates_tenant_reason UNIQUE (tenant_id, reason)
);

CREATE INDEX idx_message_templates_tenant ON message_templates(tenant_id);
