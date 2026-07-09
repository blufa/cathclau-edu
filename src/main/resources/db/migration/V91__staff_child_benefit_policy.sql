-- Politique de prise en charge des enfants du personnel (§6), paramétrable par établissement.
CREATE TABLE staff_child_benefit_policies (
    id                     BIGSERIAL PRIMARY KEY,
    tenant_id              BIGINT       NOT NULL,
    label                  VARCHAR(255) NOT NULL,
    covered_children_count INTEGER      NOT NULL,
    coverage_percent       NUMERIC(5,2) NOT NULL,
    fee_type               VARCHAR(20),
    active                 BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_staff_child_benefit_tenant ON staff_child_benefit_policies (tenant_id);
