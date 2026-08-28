-- Le numéro de téléphone du parent devient obligatoire : c'est le seul canal fiable pour les
-- notifications SMS (retard, absence, échéances). Table déjà créée en V4, aucune nouvelle table.
ALTER TABLE parents ALTER COLUMN phone SET NOT NULL;
