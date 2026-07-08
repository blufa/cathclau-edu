-- Paid plan chosen at signup, used to auto-create the subscription after email verification.
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS requested_plan_code VARCHAR(40);
