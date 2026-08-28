-- Octroi direct de permissions à un utilisateur précis, en plus de celles héritées de son rôle
-- (ex. donner "eleve:create" à un compte COMPTABLE sans créer un rôle personnalisé juste pour lui).
CREATE TABLE user_permissions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (id),
    permission_id BIGINT NOT NULL REFERENCES permissions (id),
    UNIQUE (user_id, permission_id)
);
