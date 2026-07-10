-- L'email est désormais FACULTATIF : un administrateur d'école peut se connecter avec son username
-- (ou son email s'il en a un). La contrainte NOT NULL héritée de V3 bloquait l'inscription des écoles
-- sans email. On la retire. L'unicité par tenant (uk_users_tenant_email, V16) reste valable : PostgreSQL
-- considère les valeurs NULL comme distinctes, donc plusieurs comptes sans email sont autorisés.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
