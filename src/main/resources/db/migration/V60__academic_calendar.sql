-- Module Calendrier académique : jours fériés / vacances et dates importantes (examens, échéances,
-- réunions, annonces de résultats…) rattachés à une année scolaire. Complète l'existant (années +
-- périodes/trimestres/semestres) sans le dupliquer. Tenant-scopé.

CREATE TABLE academic_events (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL,
    academic_year_id BIGINT       NOT NULL,
    title            VARCHAR(255) NOT NULL,
    event_type       VARCHAR(24)  NOT NULL DEFAULT 'OTHER',
    start_date       DATE         NOT NULL,
    end_date         DATE,
    description      TEXT,
    created_at       TIMESTAMP
);
CREATE INDEX idx_academic_events_tenant ON academic_events (tenant_id);
CREATE INDEX idx_academic_events_year ON academic_events (academic_year_id);
