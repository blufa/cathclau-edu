-- Module M: configurable plans (trial flags) + functional modules per plan and per tenant.

-- Trial flags on the plan catalogue.
ALTER TABLE plan ADD COLUMN IF NOT EXISTS is_trial      BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE plan ADD COLUMN IF NOT EXISTS trial_days    INTEGER;
ALTER TABLE plan ADD COLUMN IF NOT EXISTS default_trial BOOLEAN NOT NULL DEFAULT FALSE;

-- Modules included in a plan.
CREATE TABLE IF NOT EXISTS plan_module (
    id          BIGSERIAL PRIMARY KEY,
    plan_id     BIGINT  NOT NULL REFERENCES plan (id),
    module_code VARCHAR(32) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_plan_module UNIQUE (plan_id, module_code)
);
CREATE INDEX IF NOT EXISTS idx_plan_module_plan ON plan_module (plan_id);

-- Effective modules of a tenant (provisioned from its plan, with optional per-tenant override).
CREATE TABLE IF NOT EXISTS tenant_module (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT  NOT NULL,
    module_code     VARCHAR(32) NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    activation_date DATE,
    expiration_date DATE,
    CONSTRAINT uq_tenant_module UNIQUE (tenant_id, module_code)
);
CREATE INDEX IF NOT EXISTS idx_tenant_module_tenant ON tenant_module (tenant_id);
