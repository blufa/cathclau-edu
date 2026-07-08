-- Module M: richer plan catalogue fields (type, periodicity, extra limits, visibility, ordering).
ALTER TABLE plan ADD COLUMN IF NOT EXISTS plan_type      VARCHAR(16);
ALTER TABLE plan ADD COLUMN IF NOT EXISTS periodicity    VARCHAR(24);
ALTER TABLE plan ADD COLUMN IF NOT EXISTS user_limit     INTEGER;
ALTER TABLE plan ADD COLUMN IF NOT EXISTS sms_limit      INTEGER;
ALTER TABLE plan ADD COLUMN IF NOT EXISTS public_visible BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE plan ADD COLUMN IF NOT EXISTS display_order  INTEGER;
ALTER TABLE plan ADD COLUMN IF NOT EXISTS highlighted    BOOLEAN NOT NULL DEFAULT FALSE;
