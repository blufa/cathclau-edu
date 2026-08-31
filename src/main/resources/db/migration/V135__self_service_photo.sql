-- Self-service profil : photo pour les comptes sans fiche métier dédiée (User) et pour les
-- enseignants (Teacher n'avait pas encore de photo, contrairement à Parent et Student).
ALTER TABLE users ADD COLUMN photo VARCHAR(500);
ALTER TABLE teachers ADD COLUMN photo VARCHAR(500);
