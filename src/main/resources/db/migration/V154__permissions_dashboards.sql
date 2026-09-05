-- Extension du catalogue de permissions : tableaux de bord admin (finances/rapports, direction).
INSERT INTO permissions (name, code) VALUES
    ('Read admin finance dashboard/reports', 'dashboard:admin'),
    ('Read direction dashboard (academic, HR, founder)', 'dashboard:direction')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN' AND p.code IN ('dashboard:admin','dashboard:direction')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR' AND p.code IN ('dashboard:admin','dashboard:direction')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code = 'dashboard:admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;
