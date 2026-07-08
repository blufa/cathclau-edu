-- Module Discipline : incidents et sanctions par élève (inspiré OpenEduCat).
-- Tenant-scopé. Réutilise élèves / classes existants par identifiant ; la notification des parents
-- passe par le module communication existant (aucune duplication).

CREATE TABLE discipline_incidents (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL,
    student_id      BIGINT       NOT NULL,
    class_id        BIGINT,
    incident_date   DATE         NOT NULL,
    category        VARCHAR(24)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL,
    description     TEXT         NOT NULL,
    sanction        TEXT,
    reported_by     BIGINT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'REPORTED',
    parent_notified BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP
);
CREATE INDEX idx_discipline_tenant ON discipline_incidents (tenant_id);
CREATE INDEX idx_discipline_student ON discipline_incidents (student_id);
