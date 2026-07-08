-- Module Santé / infirmerie : fiche santé de l'élève (allergies, PAI, contacts d'urgence, médecin)
-- et passages à l'infirmerie. Référence l'élève par identifiant uniquement (pas de FK inter-module).

CREATE TABLE IF NOT EXISTS health_records (
    id                          BIGSERIAL PRIMARY KEY,
    tenant_id                   BIGINT NOT NULL,
    student_id                  BIGINT NOT NULL,
    blood_group                 VARCHAR(8),
    allergies                   TEXT,
    chronic_conditions          TEXT,
    medications                 TEXT,
    pai                         BOOLEAN NOT NULL DEFAULT FALSE,
    pai_details                 TEXT,
    dietary_restrictions        TEXT,
    doctor_name                 VARCHAR(160),
    doctor_phone                VARCHAR(40),
    emergency_contact_name      VARCHAR(160),
    emergency_contact_phone     VARCHAR(40),
    emergency_contact_relation  VARCHAR(60),
    insurance                   VARCHAR(160),
    notes                       TEXT,
    updated_at                  TIMESTAMP,
    CONSTRAINT uq_health_records_student UNIQUE (student_id)
);
CREATE INDEX IF NOT EXISTS idx_health_records_tenant ON health_records (tenant_id);

CREATE TABLE IF NOT EXISTS infirmary_visits (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    visit_date      DATE NOT NULL,
    visit_time      TIME,
    reason          TEXT,
    care_given      TEXT,
    temperature     NUMERIC(4, 1),
    outcome         VARCHAR(24) NOT NULL,
    parent_notified BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_by     BIGINT,
    created_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_infirmary_visits_tenant ON infirmary_visits (tenant_id);
CREATE INDEX IF NOT EXISTS idx_infirmary_visits_student ON infirmary_visits (student_id);
