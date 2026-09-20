-- Diffusion de masse programmée (§11.1 bonus) : déclenchée automatiquement à send_at par le job
-- planifié, sans intervention manuelle — typiquement liée à un événement école (school_event_id, sans
-- contrainte FK, les deux modules restent indépendants).

CREATE TABLE scheduled_broadcasts (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    sender_id       BIGINT,
    channel         VARCHAR(20)  NOT NULL,
    audience        VARCHAR(30)  NOT NULL,
    class_id        BIGINT,
    student_id      BIGINT,
    subject         VARCHAR(255),
    content         TEXT         NOT NULL,
    reason          VARCHAR(30),
    send_at         TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    school_event_id BIGINT,
    created_at      TIMESTAMP    NOT NULL,
    sent_at         TIMESTAMP,
    error_message   TEXT
);
CREATE INDEX idx_scheduled_broadcasts_tenant ON scheduled_broadcasts (tenant_id);
CREATE INDEX idx_scheduled_broadcasts_due ON scheduled_broadcasts (tenant_id, status, send_at);
