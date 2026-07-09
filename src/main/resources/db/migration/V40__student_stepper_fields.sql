-- Additional student dossier fields (multi-step registration: identity, address, prior schooling, health).
ALTER TABLE students ADD COLUMN IF NOT EXISTS birth_certificate_number VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS blood_group VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS landmark VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS previous_class VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS repeating BOOLEAN;
ALTER TABLE students ADD COLUMN IF NOT EXISTS transfer_reason VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS medical_notes VARCHAR(2000);
