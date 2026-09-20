-- Événement de l'école (kermesse, match, entraînement...) décidé par la direction, qui empêche les
-- élèves de suivre le cours sur une période donnée — les enseignants dont un créneau tombe dans cette
-- période restent payés sur ces heures (§payroll ↔ academics).

CREATE TABLE school_events (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    label      VARCHAR(255) NOT NULL,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    start_time TIME,
    end_time   TIME,
    reason     TEXT
);
CREATE INDEX idx_school_events_tenant ON school_events (tenant_id);
CREATE INDEX idx_school_events_dates ON school_events (tenant_id, start_date, end_date);
