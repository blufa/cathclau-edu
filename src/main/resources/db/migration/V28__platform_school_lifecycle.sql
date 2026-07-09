-- Module platform: school (tenant) lifecycle — self-service signup, email verification, TRIAL.
-- features/01-platform.md §1 (PROMPT 1).

ALTER TABLE tenant ADD COLUMN slug               VARCHAR(120);
ALTER TABLE tenant ADD COLUMN establishment_type VARCHAR(30);
ALTER TABLE tenant ADD COLUMN trial_ends_at      TIMESTAMP;
ALTER TABLE tenant ADD COLUMN activated_at       TIMESTAMP;
ALTER TABLE tenant ADD COLUMN onboarded_at       TIMESTAMP;

-- Backfill a deterministic slug for any pre-existing tenant, then enforce the global-unique constraint.
UPDATE tenant SET slug = 'ecole-' || id WHERE slug IS NULL;
ALTER TABLE tenant ALTER COLUMN slug SET NOT NULL;
ALTER TABLE tenant ADD CONSTRAINT uq_tenant_slug UNIQUE (slug);

CREATE TABLE email_verification_token (
    id          BIGSERIAL     PRIMARY KEY,
    tenant_id   BIGINT        NOT NULL REFERENCES tenant(id),
    email       VARCHAR(255)  NOT NULL,
    token       VARCHAR(255)  NOT NULL UNIQUE,
    expires_at  TIMESTAMP     NOT NULL,
    consumed_at TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evt_tenant ON email_verification_token (tenant_id);
CREATE INDEX idx_evt_email  ON email_verification_token (email);
