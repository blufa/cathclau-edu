-- Permet de choisir plusieurs types de frais à la fois (un seul, plusieurs, ou aucun = tous) pour les
-- règles de réduction familiale, avantage personnel et exonération — remplace le champ fee_type
-- (un seul type ou tous) par fee_types (liste séparée par virgules, comme installments.months).

ALTER TABLE family_discount_rules ADD COLUMN fee_types TEXT;
UPDATE family_discount_rules SET fee_types = fee_type WHERE fee_type IS NOT NULL;
ALTER TABLE family_discount_rules DROP COLUMN fee_type;

ALTER TABLE staff_child_benefit_policies ADD COLUMN fee_types TEXT;
UPDATE staff_child_benefit_policies SET fee_types = fee_type WHERE fee_type IS NOT NULL;
ALTER TABLE staff_child_benefit_policies DROP COLUMN fee_type;

ALTER TABLE exemptions ADD COLUMN fee_types TEXT;
UPDATE exemptions SET fee_types = fee_type WHERE fee_type IS NOT NULL;
ALTER TABLE exemptions DROP COLUMN fee_type;
