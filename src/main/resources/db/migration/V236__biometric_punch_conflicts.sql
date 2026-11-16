-- Détection de conflit métier (pointage hors ligne) : même sujet pointé sur deux appareils de
-- localisations différentes à quelques minutes d'intervalle -- physiquement incompatible. Jamais
-- résolu automatiquement : table de revue append-only, les deux pointages d'origine ne sont jamais
-- modifiés ni supprimés.
CREATE TABLE biometric_punch_conflicts (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    subject_type  VARCHAR(16) NOT NULL,
    subject_id    BIGINT      NOT NULL,
    punch_a_id    BIGINT      NOT NULL REFERENCES biometric_punches (id),
    punch_b_id    BIGINT      NOT NULL REFERENCES biometric_punches (id),
    device_a_id   BIGINT      NOT NULL,
    device_b_id   BIGINT      NOT NULL,
    detected_at   TIMESTAMP   NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    reviewed_by   BIGINT,
    reviewed_at   TIMESTAMP,
    review_note   TEXT
);
CREATE INDEX idx_biopunchconflict_tenant ON biometric_punch_conflicts (tenant_id);
CREATE INDEX idx_biopunchconflict_tenant_status ON biometric_punch_conflicts (tenant_id, status);
CREATE INDEX idx_biopunchconflict_subject ON biometric_punch_conflicts (subject_type, subject_id);
