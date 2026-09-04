-- Rôle DPE/DCE (Direction Préfectorale/Communale de l'Éducation) : lecture seule, cross-tenant mais
-- scopée à un périmètre géographique (voir DPE_ARCHITECTURE_ANALYSIS.md §7-8). Fondation du module de
-- partenariat DPE ; les tables de périmètre (prefectures) et le tableau de bord suivent séparément.
INSERT INTO roles (name, code, description)
VALUES ('DPE/DCE', 'DPE_ADMIN', 'Supervision en lecture seule des écoles du périmètre (préfecture ou commune)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (name, code) VALUES
    ('Read DPE dashboard', 'dpe:dashboard:read'),
    ('Read school (DPE scope)', 'dpe:school:read'),
    ('Read teacher (DPE scope)', 'dpe:teacher:read'),
    ('Read DPE alerts', 'dpe:alert:read'),
    ('Update DPE alert status', 'dpe:alert:update'),
    ('Export DPE report', 'dpe:report:export'),
    ('Compare schools (DPE scope)', 'dpe:compare:read')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'DPE_ADMIN'
  AND p.code IN ('dpe:dashboard:read','dpe:school:read','dpe:teacher:read','dpe:alert:read',
                 'dpe:alert:update','dpe:report:export','dpe:compare:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SUPER_ADMIN garde toutes les permissions (convention déjà en usage, cf. V132).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('dpe:dashboard:read','dpe:school:read','dpe:teacher:read','dpe:alert:read',
                 'dpe:alert:update','dpe:report:export','dpe:compare:read')
ON CONFLICT (role_id, permission_id) DO NOTHING;
