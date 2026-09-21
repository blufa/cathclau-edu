-- Tous les utilisateurs (parent, enseignant, personnel) se connectent par numéro de téléphone —
-- l'email ne doit plus être une condition bloquante pour créer un compte (un parent sans email doit
-- pouvoir recevoir ses identifiants par SMS). La contrainte UNIQUE existante autorise déjà plusieurs
-- lignes à NULL nativement en Postgres, aucun changement supplémentaire nécessaire sur cette contrainte.

ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
