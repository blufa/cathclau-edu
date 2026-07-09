-- Authentification par username (généré, prenom.nom) en plus de l'email, et mot de passe temporaire
-- imposant un changement à la première connexion pour les comptes créés par l'école.
ALTER TABLE users ADD COLUMN username VARCHAR(100);
ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Les comptes existants gardent l'email comme identifiant : on initialise username = email
-- pour ne pas casser leur connexion (le login accepte username OU email).
UPDATE users SET username = email WHERE username IS NULL;

ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);
CREATE INDEX idx_users_username ON users (username);
