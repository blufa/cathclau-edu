CREATE TABLE communications (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('SMS','EMAIL','WHATSAPP')),
    recipient_type VARCHAR(20) NOT NULL CHECK (recipient_type IN ('PARENT','STUDENT','TEACHER','GROUP')),
    subject VARCHAR(255),
    content TEXT NOT NULL,
    reason VARCHAR(30) CHECK (reason IN ('LATE','ABSENCE','PAYMENT_DUE','RESULT','MEETING','GENERAL')),
    sent_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE message_recipients (
    id BIGSERIAL PRIMARY KEY,
    communication_id BIGINT NOT NULL REFERENCES communications(id),
    user_id BIGINT NOT NULL,
    delivery_status VARCHAR(20) NOT NULL CHECK (delivery_status IN ('PENDING','DELIVERED','FAILED')),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    delivered_at TIMESTAMP
);

CREATE INDEX idx_communications_tenant ON communications(tenant_id);
CREATE INDEX idx_message_recipients_communication ON message_recipients(communication_id);
CREATE INDEX idx_message_recipients_user_read ON message_recipients(user_id, read);
