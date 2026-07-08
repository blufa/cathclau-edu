-- Module Inbox (§13) : notifications in-app par utilisateur (cloche de notifications).
CREATE TABLE user_notifications (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    TEXT,
    link       VARCHAR(500),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_notifications_user ON user_notifications (user_id, is_read);
