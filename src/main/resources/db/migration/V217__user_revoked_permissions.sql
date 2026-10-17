-- Révocation de permissions ciblée par utilisateur (§retour comptable) : un SUPER_ADMIN peut retirer
-- une permission précise à UN admin d'école donné, sans toucher au rôle global SCHOOL_ADMIN partagé
-- par toutes les écoles (voir user_permissions, le pendant additif de cette table).
CREATE TABLE user_revoked_permissions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (id),
    permission_id BIGINT NOT NULL REFERENCES permissions (id),
    UNIQUE (user_id, permission_id)
);
