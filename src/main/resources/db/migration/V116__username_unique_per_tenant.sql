-- L'unicité du username était GLOBALE (tenant confondus), ce qui empêchait la même personne
-- (même email/nom) d'avoir un compte enseignant/parent dans deux écoles différentes.
-- On la rend unique PAR TENANT, cohérent avec uk_users_tenant_email (V16).
ALTER TABLE users DROP CONSTRAINT uq_users_username;
DROP INDEX IF EXISTS idx_users_username;
CREATE UNIQUE INDEX uq_users_tenant_username ON users (tenant_id, username);
CREATE INDEX idx_users_username ON users (username);
