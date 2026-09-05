-- Extension du catalogue de permissions : discipline, santé/infirmerie, documents & modèles.
-- NB: HealthController référençait un rôle 'NURSE' absent de l'enum RoleCode (jamais accordable à
-- aucun utilisateur, donc mort) ; ce correctif le remplace par des permissions réellement attribuables.
INSERT INTO permissions (name, code) VALUES
    ('Manage discipline records', 'discipline:manage'),
    ('Read health records', 'sante:read'),
    ('Manage health records', 'sante:manage'),
    ('Manage document templates', 'document:manage'),
    ('Generate documents from templates', 'document:generate')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.code IN ('discipline:manage','sante:read','sante:manage','document:manage','document:generate')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR'
  AND p.code IN ('discipline:manage','sante:read','sante:manage','document:manage','document:generate')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY'
  AND p.code IN ('discipline:manage','sante:read','sante:manage','document:generate')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER'
  AND p.code IN ('discipline:manage','sante:read','document:generate')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code = 'document:generate'
ON CONFLICT (role_id, permission_id) DO NOTHING;
