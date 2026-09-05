-- Extension du catalogue de permissions : biométrie (badges/pointeuses), annonces, modèles de message.
INSERT INTO permissions (name, code) VALUES
    ('Manage biometric devices and enrollments', 'biometrique:manage'),
    ('Manage announcements', 'annonce:manage'),
    ('Manage message templates', 'modele:manage')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN' AND p.code IN ('biometrique:manage','annonce:manage','modele:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND p.code IN ('biometrique:manage','annonce:manage','modele:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SECRETARY' AND p.code IN ('biometrique:manage','annonce:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- RESPONSABLE_PEDAGOGIQUE n'a aujourd'hui aucune permission dans le catalogue (les écrans de
-- configuration des notes restent sur hasAnyRole, voir migration RBAC précédente) ; celle-ci est la
-- première qui lui corresponde réellement, sans rien changer pour les écrans non encore migrés.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'RESPONSABLE_PEDAGOGIQUE' AND p.code = 'annonce:manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'TEACHER' AND p.code = 'annonce:manage'
ON CONFLICT (role_id, permission_id) DO NOTHING;
