-- Catalogue des types d'exonération pré-configurés (§retour comptable, refonte inscription) : le
-- comptable configure une fois chaque cas de figure (ex. "Orphelin — 100%"), puis le sélectionne dans
-- une liste déroulante à l'inscription au lieu de ressaisir type/pourcentage/frais à chaque élève.
CREATE TABLE exemption_rules (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    label      VARCHAR(255) NOT NULL,
    type       VARCHAR(16)  NOT NULL,
    percentage NUMERIC(5,2),
    amount     NUMERIC(15,2),
    fee_types  TEXT,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_exemption_rules_tenant ON exemption_rules (tenant_id);
