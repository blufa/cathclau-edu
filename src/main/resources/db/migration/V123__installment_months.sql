-- Mois couverts par une échéance/tranche (§billing) : liste libre de mois "YYYY-MM", pas forcément
-- consécutifs (ex. une tranche peut couvrir Mai + Juin + Octobre + Novembre + Décembre). Stockée en
-- CSV (comme EstablishmentTypeSetConverter) — un mois "YYYY-MM" ne contient jamais de virgule.
ALTER TABLE installments ADD COLUMN IF NOT EXISTS months TEXT;
