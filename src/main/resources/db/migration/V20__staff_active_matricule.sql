-- Staff: active flag (deactivation instead of deletion) + per-tenant unique staff number
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE teachers DROP CONSTRAINT IF EXISTS uk_teachers_tenant_number;
ALTER TABLE teachers ADD CONSTRAINT uk_teachers_tenant_number UNIQUE (tenant_id, staff_number);
