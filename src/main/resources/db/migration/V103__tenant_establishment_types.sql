-- Une école peut cumuler plusieurs types d'enseignement (primaire, collège, lycée…).
-- On remplace la colonne unique establishment_type par une liste CSV establishment_types.
ALTER TABLE tenant ADD COLUMN IF NOT EXISTS establishment_types VARCHAR(255);

-- Reprise des données existantes : le type unique devient le premier (et seul) élément de la liste.
UPDATE tenant
   SET establishment_types = establishment_type
 WHERE establishment_type IS NOT NULL
   AND (establishment_types IS NULL OR establishment_types = '');

-- L'ancienne colonne est conservée en base (compatibilité), mais n'est plus mappée par l'application.
