-- Module Transport (inspiré OpenEduCat) : lignes de bus (avec véhicule/chauffeur), arrêts, et
-- abonnements des élèves. Tenant-scopé ; réutilise les élèves par identifiant. La facturation du
-- transport passe par le module billing existant (type de frais TRANSPORT) — aucun couplage.

CREATE TABLE transport_routes (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            BIGINT       NOT NULL,
    name                 VARCHAR(128) NOT NULL,
    description          TEXT,
    vehicle_registration VARCHAR(32),
    driver_name          VARCHAR(128),
    driver_phone         VARCHAR(32),
    capacity             INTEGER,
    monthly_fee          NUMERIC(15, 2),
    active               BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_transport_routes_tenant ON transport_routes (tenant_id);

CREATE TABLE transport_stops (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL,
    route_id    BIGINT       NOT NULL REFERENCES transport_routes (id) ON DELETE CASCADE,
    name        VARCHAR(128) NOT NULL,
    pickup_time TIME,
    drop_time   TIME,
    sequence    INTEGER      NOT NULL DEFAULT 10
);
CREATE INDEX idx_transport_stops_route ON transport_stops (route_id);
CREATE INDEX idx_transport_stops_tenant ON transport_stops (tenant_id);

CREATE TABLE transport_subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,
    route_id    BIGINT      NOT NULL REFERENCES transport_routes (id) ON DELETE CASCADE,
    stop_id     BIGINT      REFERENCES transport_stops (id) ON DELETE SET NULL,
    student_id  BIGINT      NOT NULL,
    direction   VARCHAR(8)  NOT NULL DEFAULT 'BOTH',
    start_date  DATE,
    monthly_fee NUMERIC(15, 2),
    status      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP
);
CREATE INDEX idx_transport_subs_tenant ON transport_subscriptions (tenant_id);
CREATE INDEX idx_transport_subs_route ON transport_subscriptions (route_id);
CREATE INDEX idx_transport_subs_student ON transport_subscriptions (student_id);
