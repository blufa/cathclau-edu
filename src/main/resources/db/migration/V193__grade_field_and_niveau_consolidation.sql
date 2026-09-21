-- Réforme du catalogue de niveaux : réduction à 3 valeurs fixes (Maternelle/Primaire/Secondaire).
-- La granularité pédagogique précise (CP1, 7ème, Terminale...) est désormais portée par la classe
-- elle-même plutôt que par le niveau. Cette migration ajoute uniquement les colonnes (nullable) ;
-- le backfill des données existantes et la consolidation des niveaux sont faits par V194, exécutée
-- une fois le code applicatif capable de lire/écrire ces colonnes.
ALTER TABLE school_classes ADD COLUMN IF NOT EXISTS grade VARCHAR(20);
ALTER TABLE school_classes ADD COLUMN IF NOT EXISTS serie VARCHAR(20);
