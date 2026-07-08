-- Règles de réduction familiale paramétrables (§5) : aucun seuil/pourcentage codé en dur.
CREATE TABLE family_discount_rules (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    label               VARCHAR(255) NOT NULL,
    min_children        INTEGER      NOT NULL,
    max_children        INTEGER,
    percentage          NUMERIC(5,2) NOT NULL,
    fee_type            VARCHAR(20),
    applies_to_youngest BOOLEAN      NOT NULL DEFAULT TRUE,
    active              BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_family_discount_rules_tenant ON family_discount_rules (tenant_id);
