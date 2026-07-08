-- Module Biometric Attendance (côté logiciel, inspiré OpenEduCat) : registre des appareils, enrôlement
-- des personnes (mapping vers l'ID interne de l'appareil), et journal des pointages (ingérés via Push API).
-- Tenant-scopé. Réutilise élèves/enseignants par identifiant.

CREATE TABLE biometric_devices (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             BIGINT       NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    device_type           VARCHAR(16)  NOT NULL DEFAULT 'FINGERPRINT',
    ip_address            VARCHAR(64),
    port                  INTEGER,
    protocol              VARCHAR(16)  NOT NULL DEFAULT 'PUSH',
    location              VARCHAR(128),
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    realtime_sync         BOOLEAN      NOT NULL DEFAULT FALSE,
    sync_interval_minutes INTEGER      NOT NULL DEFAULT 5,
    last_sync_at          TIMESTAMP,
    status                VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN',
    accuracy_level        VARCHAR(16),
    mask_allowed          BOOLEAN      NOT NULL DEFAULT TRUE,
    temperature_check     BOOLEAN      NOT NULL DEFAULT FALSE,
    temperature_threshold NUMERIC(4, 1),
    api_key               VARCHAR(64)
);
CREATE INDEX idx_biodev_tenant ON biometric_devices (tenant_id);

CREATE TABLE biometric_enrollments (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT      NOT NULL,
    device_id        BIGINT      NOT NULL REFERENCES biometric_devices (id) ON DELETE CASCADE,
    subject_type     VARCHAR(16) NOT NULL,
    subject_id       BIGINT      NOT NULL,
    biometric_type   VARCHAR(16) NOT NULL DEFAULT 'FINGERPRINT',
    external_user_id VARCHAR(64) NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at      TIMESTAMP,
    UNIQUE (device_id, external_user_id)
);
CREATE INDEX idx_bioenroll_tenant ON biometric_enrollments (tenant_id);
CREATE INDEX idx_bioenroll_device ON biometric_enrollments (device_id);

CREATE TABLE biometric_punches (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT      NOT NULL,
    device_id        BIGINT      NOT NULL,
    external_user_id VARCHAR(64) NOT NULL,
    subject_type     VARCHAR(16),
    subject_id       BIGINT,
    punch_time       TIMESTAMP   NOT NULL,
    direction        VARCHAR(8)  NOT NULL DEFAULT 'UNKNOWN',
    temperature      NUMERIC(4, 1),
    raw_payload      TEXT,
    created_at       TIMESTAMP
);
CREATE INDEX idx_biopunch_tenant ON biometric_punches (tenant_id);
CREATE INDEX idx_biopunch_device ON biometric_punches (device_id);
CREATE INDEX idx_biopunch_subject ON biometric_punches (subject_type, subject_id);
