-- Teacher professional dossier fields (employment status, diploma, remuneration, authorizations).
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS employment_status VARCHAR(255);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS diploma VARCHAR(255);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS remuneration_type VARCHAR(255);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS remuneration_rate NUMERIC(12,2);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS teaching_authorization_number VARCHAR(255);
ALTER TABLE teachers ADD COLUMN IF NOT EXISTS cnss_number VARCHAR(255);
