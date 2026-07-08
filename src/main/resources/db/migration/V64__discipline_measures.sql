-- Échelle disciplinaire du règlement intérieur : punition vs sanction, mesure précise, sursis,
-- durée d'exclusion temporaire, et instance ayant décidé (enseignant, chef, conseil de discipline,
-- commission éducative). Enrichit le module discipline existant.
ALTER TABLE discipline_incidents ADD COLUMN IF NOT EXISTS measure_kind  VARCHAR(16);
ALTER TABLE discipline_incidents ADD COLUMN IF NOT EXISTS measure_type  VARCHAR(32);
ALTER TABLE discipline_incidents ADD COLUMN IF NOT EXISTS suspension    VARCHAR(16);
ALTER TABLE discipline_incidents ADD COLUMN IF NOT EXISTS exclusion_days INTEGER;
ALTER TABLE discipline_incidents ADD COLUMN IF NOT EXISTS decision_body VARCHAR(24);
