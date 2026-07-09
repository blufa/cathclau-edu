-- DEV/TEST : compte SUPER_ADMIN pour les tests (Postman).
--   email    : super@admin.gn
--   password : Test@1234  (hash BCrypt ci-dessous)
-- ⚠️ Compte à usage de TEST : changer le mot de passe ou supprimer ce compte avant la production.
INSERT INTO users (tenant_id, first_name, last_name, email, password_hash, active, created_at)
SELECT NULL, 'Super', 'Admin', 'super@admin.gn',
       '$2a$10$uHqjCLYFZPTIX4kwSzCjwu4TT64os9ANG9xo9U6avitgFkVxPlX9e', TRUE, NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'super@admin.gn');

-- Lier le rôle SUPER_ADMIN (rôles seedés en V3).
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'SUPER_ADMIN'
WHERE u.email = 'super@admin.gn'
ON CONFLICT (user_id, role_id) DO NOTHING;
