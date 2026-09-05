-- Extension du catalogue de permissions (§RBAC rôles personnalisés) : cartes scolaires, transport,
-- devoirs, cahier de textes — modules jusqu'ici uniquement accessibles via des rôles codés en dur,
-- donc invisibles pour un rôle personnalisé (sans code fixe) même avec les bonnes permissions cochées.
INSERT INTO permissions (name, code) VALUES
    ('Manage student cards', 'carte:manage'),
    ('Manage transport', 'transport:manage'),
    ('Manage assignments', 'devoir:manage'),
    ('Manage lesson log', 'cahier:manage')
ON CONFLICT (code) DO NOTHING;

-- SCHOOL_ADMIN a déjà toutes les permissions sauf gestion plateforme (clause NOT IN de V16) : les
-- nouveaux codes ne sont pas rattrapés automatiquement, il faut les accorder explicitement ici.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN' AND p.code IN ('carte:manage','transport:manage','devoir:manage','cahier:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- DIRECTOR : mêmes 4 permissions (déjà accès via hasAnyRole aux endpoints correspondants).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND p.code IN ('carte:manage','transport:manage','devoir:manage','cahier:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SECRETARY : cartes scolaires + transport (déjà accès via hasAnyRole sur la majorité des endpoints).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY' AND p.code IN ('carte:manage','transport:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- TEACHER : devoirs + cahier de textes (déjà accès via hasAnyRole).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER' AND p.code IN ('devoir:manage','cahier:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;
