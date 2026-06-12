-- Grading: per-tenant configurable scale for honor (mention) and pass decision
CREATE TABLE grading_scales (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL UNIQUE,
    pass_threshold   NUMERIC(5,2) NOT NULL DEFAULT 10,
    passable_min     NUMERIC(5,2) NOT NULL DEFAULT 10,
    assez_bien_min   NUMERIC(5,2) NOT NULL DEFAULT 14,
    bien_min         NUMERIC(5,2) NOT NULL DEFAULT 16,
    tres_bien_min    NUMERIC(5,2) NOT NULL DEFAULT 18
);
