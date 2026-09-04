-- Extension du catalogue de permissions : budget annuel, dépenses, subventions, paie.
INSERT INTO permissions (name, code) VALUES
    ('Manage budget (annual, lines, categories, suppliers, grants)', 'budget:manage'),
    ('Approve/close budget and grants', 'budget:approve'),
    ('Create/submit expenses', 'depense:create'),
    ('Approve/reject expenses', 'depense:approve'),
    ('Manage payroll (payslips, structures, compensation)', 'paie:manage')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SCHOOL_ADMIN'
  AND p.code IN ('budget:manage','budget:approve','depense:create','depense:approve','paie:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DIRECTOR'
  AND p.code IN ('budget:manage','budget:approve','depense:approve','paie:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN ('budget:manage','depense:create','paie:manage')
ON CONFLICT (role_id, permission_id) DO NOTHING;
